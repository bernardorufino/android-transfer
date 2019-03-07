package com.brufino.android.playground.transfer;

import android.content.Context;
import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.extensions.livedata.ImmediateLiveData;
import com.brufino.android.playground.extensions.livedata.transform.Transform;
import com.brufino.android.playground.transfer.service.TransferService;
import com.brufino.android.playground.transfer.task.TaskEntry;
import com.brufino.android.playground.transfer.task.TaskInformation;
import com.brufino.android.playground.transfer.task.TaskManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.common.utils.Preconditions.checkState;
import static com.brufino.android.playground.extensions.livedata.LiveDataUtils.constantLiveDataIfNotNull;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

public class TransferManager {
    /**
     * Quick summary. Putting it with 1 maximizes throughput in the beginning, increasing it (to
     * 5, then 10) hurts throughput in the beginning. Although all throughput values after some
     * time tend to be relatively the same (needs more investigation, just feeling) and go down.
     * Maybe something else is playing here (the history size might be delaying some stuff).
     *
     * This all mean that the main-thread is being used as a front-line queue, instead of
     * TransferService queue. We have to put items in the queue directly from TransferManager to
     * bypass this.
     */
    private static final int MAX_THREADS_REQUESTING_TRANSFER = 1;
    private static final int SEMAPHORE_PERMITS = MAX_THREADS_REQUESTING_TRANSFER;
    private static final int FIRST_START_ID = 0;
    static final int IDLE_LAST_START_ID = FIRST_START_ID - 1;

    private final Context mContext;
    private final TaskManager mTaskManager;
    private final LiveData<List<TransferRequest>> mLiveQueue;
    private final LiveData<Double> mLiveThroughput;

    /** Semaphore allows more than 1 thread in certain code-paths. */
    private final Semaphore mRequestSemaphore = new Semaphore(SEMAPHORE_PERMITS, true);

    /** Any set operation on this requires acquiring SEMAPHORE_PERMITS from mRequestSemaphore. */
    @GuardedBy("mRequestSemaphore")
    private final ImmediateLiveData<Optional<TransferManagerService>> mLiveService;

    /**
     * Even though we are guarded by the semaphore we need an atomic because the semaphore
     * allows multiple threads executing.
     */
    @GuardedBy("mRequestSemaphore")
    private final AtomicInteger mNextStartId = new AtomicInteger(FIRST_START_ID);

    @GuardedBy("mRequestSemaphore")
    private boolean mStopping = false;

    @MainThread
    public TransferManager(
            ApplicationContext context,
            TaskManager taskManager,
            ExecutorService workExecutor) {
        mContext = context.getContext();
        mTaskManager = taskManager;
        mLiveService = new ImmediateLiveData<>(empty(), workExecutor);
        mLiveQueue =
                Transform.source(mLiveService.getLiveData())
                        .optional()
                        .castToOptionalOf(TransferService.class, workExecutor)
                        // TODO(brufino): In workExecutor!
                        .switchMapIfPresent(TransferService::getLiveQueue)
                        .orElse(emptyList())
                        .getLiveData();
        mLiveThroughput =
                Transform.source(mLiveService.getLiveData())
                        .optional()
                        .castToOptionalOf(TransferService.class, workExecutor)
                        .<Double>switchMap(
                                (service, oldThroughput) ->
                                        service
                                                .map(TransferService::getLiveThroughput)
                                                .orElse(constantLiveDataIfNotNull(oldThroughput)),
                                workExecutor)
                        .getLiveData();

    }

    public LiveData<Optional<TaskInformation>> getLiveTaskInformation() {
        return mTaskManager.getTaskInformation();
    }

    public LiveData<List<TaskEntry>> getLiveHistory() {
        return mTaskManager.getLiveHistory();
    }

    public LiveData<List<TransferRequest>> getLiveQueue() {
        return mLiveQueue;
    }

    public LiveData<Double> getLiveTaskThroughput() {
        return mLiveThroughput;
    }

    public void clearHistory() {
        mTaskManager.clearHistory();
    }

    public void clearQueue() throws InterruptedException {
        mRequestSemaphore.acquire();
        try {
            Optional<TransferManagerService> serviceOptional =
                    checkNotNull(mLiveService.getValue());
            if (serviceOptional.isPresent()) {
                TransferService service = (TransferService) serviceOptional.get();
                service.clearQueue();
            }
        } finally {
            mRequestSemaphore.release();
        }

    }

    /**
     * Implementation note: We limit the number of threads that can submit a request in parallel,
     * otherwise all those requests would end up overwhelming the main-thread, that puts them in
     * a queue, among other things.
     */
    public void enqueueTransfer(
            @Code int code, TransferConfiguration configuration) throws InterruptedException {
        TransferRequest request = TransferRequest.create(code, configuration);
        mRequestSemaphore.acquire();
        try {
            Optional<TransferManagerService> service = checkNotNull(mLiveService.getValue());
            if (service.isPresent()) {
                int startId = mNextStartId.getAndIncrement();
                service.get().request(request, startId);
                Log.d("BML", "request(TRANSFER_MANAGER/" + startId + ")");
                return;
            }
        } finally {
            mRequestSemaphore.release();
        }
        int startId = mNextStartServiceId.getAndIncrement();
        Log.d("BML", "request(START_SERVICE/" + startId + ")");
        mContext.startForegroundService(request.getIntent(mContext));
    }

    private final AtomicInteger mNextStartServiceId = new AtomicInteger(0);

    /**
     * Tries to begin stopping the service. If {@code startId} was the last one issued for a
     * transfer request via {@link #enqueueTransfer(int, TransferConfiguration)} and subsequent
     * {@link TransferService#request(TransferRequest, int)} then returns true and allows the
     * caller to stop the service.
     *
     * <p>Returning {@code true} means that the caller MUST call {@link #finishStopService(boolean)}
     * at some point. Note however that the caller still has the chance of aborting the stop by
     * passing {@code false} to {@link #finishStopService(boolean)}.
     *
     * <p>The caller MUST NOT call {@link #finishStopService(boolean)} if {@code false} is
     * returned from this method.
     */
    boolean tryBeginStopService(int startId) throws InterruptedException {
        mRequestSemaphore.acquire(SEMAPHORE_PERMITS);
        Log.d("BMReq", "TM.tryBeginStopService(TRANSFER_MANAGER/" + startId + ")");
        if (startId != mNextStartId.get() - 1) {
            mRequestSemaphore.release(SEMAPHORE_PERMITS);
            return false;
        }
        mStopping = true;
        return true;
    }

    /**
     * Finishes stopping the service if {@code stop} is true, otherwise aborts and the service
     * continues running (unless stopped by other means).
     */
    void finishStopService(boolean stop) {
        checkState(
                mStopping,
                "finishStopService() MUST NOT be called unless tryBeginStopService() was called AND "
                        + "returned true.");

        Log.d("BMReq", "TM.finishStopService(stop = " + stop + ")");
        try {
            if (stop) {
                mLiveService.setValue(empty());
            }
            mStopping = false;
        } finally {
            mRequestSemaphore.release(SEMAPHORE_PERMITS);
        }
    }

    @MainThread
    void onServiceCreated(TransferManagerService service) throws InterruptedException {
        mRequestSemaphore.acquire(SEMAPHORE_PERMITS);
        Log.d("BMReq",
                "onServiceCreated(" + Integer.toHexString(System.identityHashCode(service)) + ")");
        try {
            mLiveService.setValue(Optional.of(service));
        } finally {
            mRequestSemaphore.release(SEMAPHORE_PERMITS);
        }
    }

    @MainThread
    void onServiceDestroy() throws InterruptedException {
        mRequestSemaphore.acquire(SEMAPHORE_PERMITS);
        Log.d("BMReq", "onServiceDestroy()");
        try {
            // It should already be empty unless the service wasn't cleanly destroyed (i.e.
            // called tryBeginStopService() and finishStopService()).
            mLiveService.setValue(empty());
        } finally {
            mRequestSemaphore.release(SEMAPHORE_PERMITS);
        }
    }

    @IntDef({
            Code.SINGLE_THREAD,
            Code.MULTI_THREAD,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Code {
        int SINGLE_THREAD = 0;
        int MULTI_THREAD = 1;
    }
}
