package com.brufino.android.playground.transfer.task;

import android.content.Context;
import android.util.Log;
import androidx.annotation.GuardedBy;
import com.brufino.android.common.CommonConstants;
import com.brufino.android.playground.extensions.ApplicationContext;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static com.brufino.android.common.CommonConstants.TAG;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.*;

public class TaskHistory {
    private static final String HISTORY_FILE_NAME = "history.txt";

    private final Context mContext;
    private final Executor mIoExecutor;
    private final Executor mWorkExecutor;
    private final Object mSaveLock = new Object();

    @GuardedBy("mSaveLock")
    private List<TaskEntry> mPending;

    @GuardedBy("mSaveLock")
    private boolean mSaving;

    public TaskHistory(
            ApplicationContext context,
            ExecutorService ioExecutor,
            ExecutorService workExecutor) {
        mContext = context.getContext();
        mIoExecutor = ioExecutor;
        mWorkExecutor = workExecutor;
    }

    @SuppressWarnings("unchecked")
    List<TaskEntry> open() {
        try (ObjectInputStream input =
                     new ObjectInputStream(new FileInputStream(getFile(mContext)))) {
            return (List<TaskEntry>) input.readObject();
        } catch (NoSuchFileException | FileNotFoundException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            Log.e(TAG, "IOException while trying to load history", e);
            return new ArrayList<>();
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /** The operation doesn't happen inline. */
    void save(List<TaskEntry> tasks) {
        // Since the operation doesn't happen inline we need to guarantee immutability of the list
        tasks = new CopyOnWriteArrayList<>(tasks);
        synchronized (mSaveLock) {
            if (mSaving) {
                mPending = tasks;
            } else {
                mSaving = true;
                triggerSaveLocked(tasks);
            }
        }
    }

    @GuardedBy("mSaveLock")
    private void triggerSaveLocked(List<TaskEntry> tasks) {
        execute(mIoExecutor, () -> onSave(tasks))
                .exceptionally(
                        catching(
                                IOException.class,
                                e -> Log.e(TAG, "Error persisting history", e)))
                .exceptionally(throwIn(getMainThreadExecutor()));

    }

    private void onSave(List<TaskEntry> tasks) throws IOException {
        try (ObjectOutputStream output =
                     new ObjectOutputStream(new FileOutputStream(getFile(mContext)))) {
            output.writeObject(tasks);
        } finally {
            onSaveFinished();
        }
    }

    private void onSaveFinished() {
        synchronized (mSaveLock) {
            if (mPending == null) {
                mSaving = false;
            } else {
                triggerSaveLocked(mPending);
                mPending = null;
            }
        }
    }

    private static File getFile(Context context) {
        return context.getFilesDir().toPath().resolve(HISTORY_FILE_NAME).toFile();
    }
}
