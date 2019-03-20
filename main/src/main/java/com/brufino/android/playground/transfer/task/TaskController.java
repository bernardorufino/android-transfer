package com.brufino.android.playground.transfer.task;

import android.Manifest;
import android.os.DeadObjectException;
import android.os.Debug;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import com.brufino.android.common.IConsumer;
import com.brufino.android.common.IProducer;
import com.brufino.android.playground.MainConstants;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.extensions.livedata.ImmediateLiveData;
import com.brufino.android.playground.extensions.permission.PermissionUtils;
import com.brufino.android.playground.transfer.TransferConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static com.brufino.android.common.CommonConstants.TAG;
import static com.brufino.android.common.utils.Preconditions.checkState;
import static com.brufino.android.playground.extensions.AndroidUtils.getDocumentPath;

/** CAUTION: All these methods may be called from different threads. */
public class TaskController {
    private final Object mMeasurementsLock = new Object();
    private final ApplicationContext mContext;
    private final TransferConfiguration mConfiguration;
    private final LifecycleRegistry mLifecycleRegistry;
    private final Path mTraceFile;
    private final ImmediateLiveData<TaskInformation> mLiveTaskInformation;

    @GuardedBy("mMeasurementsLock")
    private final Map<String, Collection<Long>> mMeasurements = new ArrayMap<>();

    TaskController(
            ApplicationContext context,
            String name,
            TransferConfiguration configuration,
            LifecycleRegistry lifecycleRegistry,
            ImmediateLiveData<TaskInformation> liveTaskInformation) {
        mContext = context;
        mConfiguration = configuration;
        mLifecycleRegistry = lifecycleRegistry;
        mTraceFile = getDocumentPath(name + ".trace");
        mLiveTaskInformation = liveTaskInformation;
    }

    public Stopwatch startTime(String label) {
        return Stopwatch.now(
                duration -> {
                    synchronized (mMeasurementsLock) {
                        mMeasurements.computeIfAbsent(label, l -> new ArrayList<>()).add(duration);
                    }
                });
    }

    public void configure(IProducer producer) throws RemoteException {
        checkState(
                mLifecycleRegistry.getCurrentState() == Lifecycle.State.STARTED,
                "Can only configure after started");

        producer.configure(
                mConfiguration.producerDataSize,
                mConfiguration.producerChunkSize,
                mConfiguration.producerInterval);
        // TODO(brufino): Only update when both updated.
        mLiveTaskInformation.updateValue(
                taskInformation -> taskInformation.setConfiguration(mConfiguration));
    }

    public void configure(IConsumer consumer) throws RemoteException {
        checkState(
                mLifecycleRegistry.getCurrentState() == Lifecycle.State.STARTED,
                "Can only configure after started");

        consumer.configure(
                mConfiguration.consumerBufferSize, mConfiguration.consumerInterval);
        // TODO(brufino): Only update when both updated.
        mLiveTaskInformation.updateValue(
                taskInformation -> taskInformation.setConfiguration(mConfiguration));
    }

    public int getBufferSize() {
        return mConfiguration.transferBufferSize;
    }

    public void addInputRead(int sizeRead) {
        mLiveTaskInformation.updateValue(
                taskInformation -> taskInformation.addInputRead(sizeRead));
    }

    public void addOutputWritten(int sizeWritten) {
        mLiveTaskInformation.updateValue(
                taskInformation -> taskInformation.addOutputWritten(sizeWritten));
    }

    public boolean startTracing() {
        if (!MainConstants.DEBUG || Files.exists(mTraceFile)) {
            return false;
        }
        if (!PermissionUtils.hasPermission(
                mContext.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.e(
                    TAG,
                    String.format(
                            "Can't record %s due to no WRITE_EXTERNAL_STORAGE permission",
                            mTraceFile));
            return false;
        }
        Debug.startMethodTracing(mTraceFile.toString());
        return true;
    }

    public void stopTracing(boolean tracing) {
        if (MainConstants.DEBUG && tracing) {
            Debug.stopMethodTracing();
        }
    }

    Map<String, Collection<Long>> getMeasurements() {
        return mMeasurements;
    }

    public static class Stopwatch {
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
