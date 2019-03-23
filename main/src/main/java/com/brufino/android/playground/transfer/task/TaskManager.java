package com.brufino.android.playground.transfer.task;

import android.content.Context;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.*;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.extensions.concurrent.HandlerExecutor;
import com.brufino.android.playground.extensions.livedata.transform.Transform;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.transfer.task.tasks.TaskFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import static com.brufino.android.common.CommonConstants.TAG;
import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.playground.extensions.livedata.LiveDataUtils.computableLiveData;

public class TaskManager {
    private static final String THREAD_NAME = "task-manager";

    private final Object mTaskLock = new Object();
    private final Context mContext;
    private final Looper mLooper;
    private final MutableLiveData<Optional<TransferTask>> mLiveTask = new MutableLiveData<>();
    private final LiveData<Optional<TaskInformation>> mTaskInformation;
    private final ComputableLiveData<List<TaskEntry>> mLiveHistory;
    private final TaskFactory mTaskFactory;
    private final TaskHistory mTaskHistory;

    @GuardedBy("mTaskLock")
    private final List<TaskEntry> mHistory;

    @GuardedBy("mTaskLock")
    @Nullable
    private TransferTask mTask;


    @MainThread
    public TaskManager(
            ApplicationContext context,
            TaskFactory taskFactory,
            TaskHistory taskHistory) {
        mTaskInformation =
                Transform.source(mLiveTask)
                        .<TransferTask>optional()
                        .switchMapIfPresent(TransferTask::getLiveTaskInformation)
                        .getLiveData();
        mContext = context.getContext();
        mTaskFactory = taskFactory;
        mTaskHistory = taskHistory;
        HandlerThread thread = new HandlerThread(THREAD_NAME);
        thread.start();
        mLooper = thread.getLooper();
        mHistory = new ArrayList<>(taskHistory.open());
        mLiveHistory = computableLiveData(HandlerExecutor.forLooper(mLooper), this::getHistory);
    }

    private List<TaskEntry> getHistory() {
        synchronized (mTaskLock) {
            return new ArrayList<>(mHistory);
        }
    }

    public LiveData<Optional<TaskInformation>> getTaskInformation() {
        return mTaskInformation;
    }

    public LiveData<List<TaskEntry>> getLiveHistory() {
        return mLiveHistory.getLiveData();
    }

    public void clearHistory() {
        synchronized (mTaskLock) {
            mHistory.clear();
            onHistoryChangedLocked();
        }
    }

    @GuardedBy("mTaskLock")
    private void onHistoryChangedLocked() {
        mLiveHistory.invalidate();
        mTaskHistory.save(new CopyOnWriteArrayList<>(mHistory));
    }

    @GuardedBy("mTaskLock")
    private void setTaskLocked(@Nullable TransferTask task) {
        mTask = task;
        mLiveTask.postValue(Optional.ofNullable(task));
    }

    public TransferTask startTask(
            @TransferManager.Code int code,
            TransferConfiguration configuration) throws ConcurrentTaskException {
        synchronized (mTaskLock) {
            if (mTask != null) {
                throw new ConcurrentTaskException(
                        "Task " + mTask + " already running, can't start another");
            }
            TransferTask task = mTaskFactory.getTask(code, configuration, mLooper);
            Lifecycle taskLifecycle = task.getLifecycle();
            taskLifecycle.addObserver(new TaskObserver());
            Log.d(TAG, "Triggering task " + task.getName() + " with " + configuration);
            task.trigger();
            setTaskLocked(task);
            return task;
        }
    }

    /** Cancels the currently running task. */
    @Nullable
    public CompletableFuture<Void> cancelTask() throws NoTaskRunningException {
        final TransferTask task;
        synchronized (mTaskLock) {
            if (mTask == null) {
                throw new NoTaskRunningException("Can't cancel task.");
            }
            task = mTask;
        }
        return task.cancel();
    }

    /**
     * If the main-thread is very busy, when for example there are 1000+ tasks and the history
     * fragment has just too much text, the task will slow down in places where we have callbacks
     * depending on the main-thread, such as bindService() and its connection.
     */
    private class TaskObserver implements DefaultLifecycleObserver {
        @Override
        public void onStop(LifecycleOwner owner) {
            synchronized (mTaskLock) {
                checkNotNull(mTask);
                TaskInformation information = checkNotNull(mTask.getTaskInformation());
                Map<String, TaskMeasurement> measurements = mTask.getMeasurements();
                mHistory.add(
                        new TaskEntry(
                                information.name,
                                information.getDuration(),
                                information.inputRead,
                                information.outputWritten,
                                information.configuration,
                                measurements,
                                information.exception));
                onHistoryChangedLocked();
                setTaskLocked(null);
            }
        }
    }

    public static class ConcurrentTaskException extends Exception {
        private ConcurrentTaskException(String message) {
            super(message);
        }
    }

    public static class NoTaskRunningException extends Exception {
        private NoTaskRunningException(String message) {
            super(message);
        }
    }
}
