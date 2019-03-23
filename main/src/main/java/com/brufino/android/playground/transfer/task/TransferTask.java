package com.brufino.android.playground.transfer.task;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.LiveData;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.extensions.concurrent.HandlerExecutor;
import com.brufino.android.playground.extensions.livedata.ImmediateLiveData;
import com.brufino.android.playground.transfer.TransferCancellationFailedException;
import com.brufino.android.playground.transfer.TransferCancelledException;
import com.brufino.android.playground.transfer.TransferConfiguration;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.common.utils.Preconditions.checkState;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.asyncThrowingFunction;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.catching;
import static java.util.stream.Collectors.toMap;

public abstract class TransferTask implements LifecycleOwner {
    public static final String PRODUCER_PACKAGE = "com.brufino.android.producer";
    public static final String CONSUMER_PACKAGE = "com.brufino.android.consumer";
    public static final long TASK_TIME_OUT_MS = 10_000;

    private final CompletableFuture<Void> mResult = new CompletableFuture<>();
    private final ApplicationContext mContext;
    private final Handler mHandler;
    private final Executor mExecutor;
    private final String mName;
    private final TransferConfiguration mConfiguration;
    private final LifecycleRegistry mLifecycleRegistry;
    private final ImmediateLiveData<TaskInformation> mLiveTaskInformation;
    private final TaskController mController;
    private volatile boolean mCancelRequested;

    /**
     * This is the task's private cancel mechanism, which will be ignored if the subclass
     * implements {@link #onCancel()}.
     *
     * MUST only be accessed from the mHandler's looper thread.
     *
     * @see #getTerminationException(Exception) 
     */
    private boolean mPrivateCancel;


    public TransferTask(
            ApplicationContext context,
            Looper looper,
            TransferConfiguration configuration,
            String name) {
        mContext = context;
        mHandler = new Handler(looper);
        mExecutor = new HandlerExecutor(mHandler);
        mName = name;
        // TODO(brufino): Verify configuration
        mConfiguration = configuration;
        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.markState(Lifecycle.State.INITIALIZED);
        mLiveTaskInformation = new ImmediateLiveData<>(mExecutor);
        mController =
                new TaskController(
                        context,
                        name,
                        configuration,
                        mLifecycleRegistry,
                        mLiveTaskInformation);
        mCancelRequested = false;
        mPrivateCancel = false;
    }

    public String getName() {
        return mName;
    }

    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    public void join() {
        mResult.exceptionally(t -> null).join();
    }

    /**
     * Future returned will complete normally if cancellation happens, otherwise it will complete
     * exceptionally. Note that if the task completed successfully this future will complete with
     * exception {@link TransferCancellationFailedException}.
     */
    public CompletableFuture<Void> cancel() {
        mCancelRequested = true;
        mHandler.post(this::onCancel);
        return mResult
                .<Void>thenApplyAsync(
                        // Normal completion becomes TaskCancellationFailedException
                        asyncThrowingFunction(TransferCancellationFailedException::new), mExecutor)
                .exceptionally(
                        // Task cancellation becomes normal completion
                        catching(e -> null, TransferCancelledException.class));

    }

    /** When this method is called the task is already in STARTED state. */
    protected abstract void onStart();

    /** When this method is called the task is already in STOPPED/CREATED state. */
    @SuppressWarnings("WeakerAccess")
    protected void onStop() {}

    /**
     * Override to implement cancellation.
     *
     * If not overridden a cancel request will result in the task effectively completing by the
     * implementation but being considered cancelled, i.e. terminated with a
     * {@link TransferCancelledException}.
     */
    @SuppressWarnings("WeakerAccess")
    protected void onCancel() {
        mPrivateCancel = true;
    }

    TaskInformation getTaskInformation() {
        return mLiveTaskInformation.getValue();
    }

    LiveData<TaskInformation> getLiveTaskInformation() {
        // The returned value is final deep inside, so no need to lock on anything to return this.
        return mLiveTaskInformation.getLiveData();
    }

    /**
     * Will compute them in this method, please cache the result. Only call this after task has
     * finished.
     */
    Map<String, TaskMeasurement> getMeasurements() {
        checkState(mLifecycleRegistry.getCurrentState() == Lifecycle.State.CREATED);
        //noinspection OptionalGetWithoutIsPresent
        return mController
                .getMeasurements()
                .entrySet()
                .stream()
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                entry ->
                                        TaskMeasurement.create(
                                                entry.getKey(),
                                                entry.getValue())));
    }

    void trigger() {
        checkState(
                 mLifecycleRegistry.getCurrentState() == Lifecycle.State.INITIALIZED,
                "Task already executed.");

        mHandler.post(
                () -> {
                    mLiveTaskInformation.setValue(TaskInformation.started(mName, mConfiguration));
                    mLifecycleRegistry.markState(Lifecycle.State.STARTED);
                    onStart();
                });
    }

    protected Handler getHandler() {
        return mHandler;
    }

    protected Executor getExecutor() {
        return mExecutor;
    }

    protected TaskController getController() {
        return mController;
    }

    /** Task successfully finished. */
    protected void finishTask() {
        terminateTask(null);
    }

    /** Task failed. */
    protected void abortTask(Exception exception) {
        terminateTask(checkNotNull(exception));
    }

    /** Task fulfilled cancel. */
    @SuppressWarnings("unused")
    protected void markTaskCancelled() {
        checkState(mCancelRequested, "Task was cancelled but there was no cancel request.");
        terminateTask(new TransferCancelledException());
    }

    private void terminateTask(@Nullable Exception exception) {
        mHandler.post(
                () -> {
                    Exception finalException = getTerminationException(exception);
                    mLiveTaskInformation
                            .updateValue(taskInformation -> taskInformation.setEnded(finalException));
                    mLifecycleRegistry.markState(Lifecycle.State.CREATED);
                    if (finalException == null) {
                        mResult.complete(null);
                    } else {
                        mResult.completeExceptionally(finalException);
                    }
                    onStop();
                });
        // Cannot block on mResult here because this can be called from onStart() and would
        // deadlock
    }

    private Exception getTerminationException(@Nullable Exception exception) {
        if (exception == null && mPrivateCancel) {
            // Task completed with a cancel request mid-flight not handled by the implementation.
            // This means the task practically completes but is considered cancelled.
            return new TransferCancelledException();
        }
        return exception;
    }

}
