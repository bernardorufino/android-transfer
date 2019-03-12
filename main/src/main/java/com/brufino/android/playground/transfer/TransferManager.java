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
import static com.brufino.android.playground.extensions.livedata.LiveDataUtils.constantLiveDataIfNotNull;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

public class TransferManager {
    private static final int MAX_THREADS_REQUESTING_TRANSFER = 10;
    private static final int SEMAPHORE_PERMITS = MAX_THREADS_REQUESTING_TRANSFER;

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
    private final AtomicInteger mNextStartId = new AtomicInteger(0);

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
                        .switchMapIfPresent(TransferService::getLiveQueue, workExecutor)
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
            Optional<TransferManagerService> service = checkNotNull(mLiveService.getValue());
            if (service.isPresent()) {
                service.get().clear();
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
                return;
            }
        } finally {
            mRequestSemaphore.release();
        }
        mContext.startForegroundService(request.getIntent(mContext));
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

    /**
     * After this method returns any request made via {@link #enqueueTransfer(int,
     * TransferConfiguration)} will be fulfilled via Android start-service infra-structure, *not*
     * directly via {@link TransferManagerService#request(TransferRequest, int)}.
     *
     * Also, after this method returns there won't be any direct calls to the service.
     */
    @MainThread
    void onServiceDestroy() throws InterruptedException {
        mRequestSemaphore.acquire(SEMAPHORE_PERMITS);
        Log.d("BMReq", "onServiceDestroy()");
        try {
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
