package com.brufino.android.playground.transfer.task;

import android.Manifest;
import android.os.*;
import android.util.ArrayMap;
import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.lifecycle.*;
import com.brufino.android.common.CommonConstants;
import com.brufino.android.common.IConsumer;
import com.brufino.android.common.IProducer;
import com.brufino.android.playground.MainConstants;
import com.brufino.android.playground.extensions.concurrent.HandlerExecutor;
import com.brufino.android.playground.extensions.livedata.ImmediateLiveData;
import com.brufino.android.playground.extensions.permission.PermissionUtils;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.extensions.ApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static com.brufino.android.common.utils.Preconditions.checkState;
import static com.brufino.android.playground.extensions.AndroidUtils.getDocumentPath;
import static com.brufino.android.playground.extensions.livedata.LiveDataUtils.computableLiveData;
import static java.util.stream.Collectors.toMap;

public abstract class TransferTask implements LifecycleOwner {
    public static final String PRODUCER_PACKAGE = "com.brufino.android.producer";
    public static final String CONSUMER_PACKAGE = "com.brufino.android.consumer";
    protected static final long PRODUCER_TIME_OUT_MS = 10_000;

    private final ConditionVariable mFinished = new ConditionVariable(false);
    private final Object mMeasurementsLock = new Object();
    private final ApplicationContext mContext;
    private final Handler mHandler;
    private final String mName;
    private final TransferConfiguration mConfiguration;
    private final LifecycleRegistry mLifecycleRegistry;
    private final Path mTraceFile;
    private final ImmediateLiveData<TaskInformation> mLiveTaskInformation;

    @GuardedBy("mMeasurementsLock")
    private final Map<String, Collection<Long>> mMeasurements = new ArrayMap<>();

    public TransferTask(
            ApplicationContext context,
            Looper looper,
            TransferConfiguration configuration,
            String name) {
        mContext = context;
        mHandler = new Handler(looper);
        mName = name;
        mConfiguration = configuration;
        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.markState(Lifecycle.State.INITIALIZED);
        mTraceFile = getDocumentPath(name + ".trace");
        mLiveTaskInformation = new ImmediateLiveData<>(new HandlerExecutor(mHandler));
    }

    protected abstract void onStart();

    protected void onStop() {}

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
            return mMeasurements
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
            mLiveTaskInformation.setValue(
                    new TaskInformation(mName, System.currentTimeMillis(), 0, 0, mConfiguration));
            mLifecycleRegistry.markState(Lifecycle.State.STARTED);
            onStart();
        });
    }

    public void join() {
        mFinished.block();
    }

    protected void finishTask() {
        mHandler.post(() -> {
            onStop();
            mLifecycleRegistry.markState(Lifecycle.State.CREATED);
            mFinished.open();
        });
        // Cannot block on mFinished here because this can be called from onStart() and would
        // deadlock
    }

    protected ApplicationContext getApplicationContext() {
        return mContext;
    }

    protected Stopwatch startTime(String label) {
        return Stopwatch.now(
                duration -> {
                    synchronized (mLiveTaskInformation) {
                        mMeasurements.computeIfAbsent(label, l -> new ArrayList<>()).add(duration);
                    }
                });
    }

    protected void configure(IProducer producer, IConsumer consumer) throws RemoteException {
        checkState(
                mLifecycleRegistry.getCurrentState() == Lifecycle.State.STARTED,
                "Can only configure after started");

        producer.configure(
                mConfiguration.producerDataSize,
                mConfiguration.producerChunkSize,
                mConfiguration.producerInterval);
        consumer.configure(mConfiguration.consumerBufferSize, mConfiguration.consumerInterval);
        mLiveTaskInformation.updateValue(
                taskInformation -> taskInformation.setConfiguration(mConfiguration));
    }

    protected int getBufferSize() {
        return mConfiguration.transferBufferSize;
    }

    protected void inputRead(int sizeRead) {
        mLiveTaskInformation.updateValue(
                taskInformation -> taskInformation.addInputRead(sizeRead));
    }

    protected void outputWritten(int sizeWritten) {
        mLiveTaskInformation.updateValue(
                taskInformation -> taskInformation.addOutputWritten(sizeWritten));
    }

    protected boolean startTracing() {
        if (!MainConstants.DEBUG || Files.exists(mTraceFile)) {
            return false;
        }
        if (!PermissionUtils.hasPermission(
                mContext.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.e(
                    CommonConstants.TAG,
                    String.format(
                            "Can't record %s due to no WRITE_EXTERNAL_STORAGE permission",
                            mTraceFile));
            return false;
        }
        Debug.startMethodTracing(mTraceFile.toString());
        return true;
    }

    protected void stopTracing(boolean tracing) {
        if (MainConstants.DEBUG && tracing) {
            Debug.stopMethodTracing();
        }
    }

    protected static class Stopwatch {
        private static Stopwatch now(Consumer<Long> onStopListener) {
            return new Stopwatch(System.nanoTime(), onStopListener);
        }

        private final long mStartTimeNanos;
        private final Consumer<Long> mOnStopListener;
        private long mStopTimeNanos;

        private Stopwatch(long startTimeNanos, Consumer<Long> onStopListener) {
            mStartTimeNanos = startTimeNanos;
            mOnStopListener = onStopListener;
            mStopTimeNanos = -1;
        }

        public void stop() {
            mStopTimeNanos = System.nanoTime();
            mOnStopListener.accept(mStopTimeNanos - mStartTimeNanos);
        }
    }
}
