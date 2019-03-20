package com.brufino.android.playground.transfer.task;

import android.os.*;
import android.util.ArrayMap;
import androidx.annotation.GuardedBy;
import androidx.lifecycle.*;
import com.brufino.android.playground.extensions.concurrent.HandlerExecutor;
import com.brufino.android.playground.extensions.livedata.ImmediateLiveData;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.extensions.ApplicationContext;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static com.brufino.android.common.utils.Preconditions.checkState;
import static com.brufino.android.playground.extensions.livedata.LiveDataUtils.computableLiveData;
import static java.util.stream.Collectors.toMap;

public abstract class TransferTask implements LifecycleOwner {
    public static final String PRODUCER_PACKAGE = "com.brufino.android.producer";
    public static final String CONSUMER_PACKAGE = "com.brufino.android.consumer";
    public static final long TASK_TIME_OUT_MS = 10_000;

    private final ConditionVariable mFinished = new ConditionVariable(false);
    private final Object mMeasurementsLock = new Object();
    private final ApplicationContext mContext;
    private final Handler mHandler;
    private final Executor mExecutor;
    private final String mName;
    private final TransferConfiguration mConfiguration;
    private final LifecycleRegistry mLifecycleRegistry;
    private final ImmediateLiveData<TaskInformation> mLiveTaskInformation;
    private final TaskController mController;

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
    }

    /** When this method is called the task is already in STARTED state. */
    protected abstract void onStart();

    /** When this method is called the task is already in STOPPED/CREATED state. */
    protected void onStop() {}

    protected Handler getHandler() {
        return mHandler;
    }

    protected Executor getExecutor() {
        return mExecutor;
    }

    public String getName() {
        return mName;
    }

    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
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
        synchronized (mMeasurementsLock) {
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
    }

    void trigger() {
        checkState(
                 mLifecycleRegistry.getCurrentState() == Lifecycle.State.INITIALIZED,
                "Task already executed.");

        mHandler.post(() -> {
            mLiveTaskInformation.setValue(TaskInformation.started(mName, mConfiguration));
            mLifecycleRegistry.markState(Lifecycle.State.STARTED);
            onStart();
        });
    }

    public void join() {
        mFinished.block();
    }

    protected void finishTask() {
        mHandler.post(() -> {
            mLiveTaskInformation.updateValue(TaskInformation::setEnded);
            mLifecycleRegistry.markState(Lifecycle.State.CREATED);
            mFinished.open();
            onStop();
        });
        // Cannot block on mFinished here because this can be called from onStart() and would
        // deadlock
    }

    protected TaskController getController() {
        return mController;
    }
}
