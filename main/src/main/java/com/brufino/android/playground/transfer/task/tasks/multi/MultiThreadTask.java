package com.brufino.android.playground.transfer.task.tasks.multi;

import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.transfer.task.TaskController;
import com.brufino.android.playground.transfer.task.TransferTask;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.transfer.task.tasks.multi.subtasks.ProducerReaderSubTask;
import com.brufino.android.playground.transfer.task.tasks.multi.subtasks.MultiSubTaskFactory;
import com.brufino.android.playground.transfer.task.tasks.multi.subtasks.ConsumerWriterSubTask;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.catching;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.execute;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.throwIn;

public class MultiThreadTask extends TransferTask {
    private final MultiSubTaskFactory mSubTaskFactory;
    private final TaskController mController;
    private final ExecutorService mTaskExecutor;
    private ProducerReaderSubTask mReaderTask;
    private ConsumerWriterSubTask mWriterTask;

    public MultiThreadTask(
            ApplicationContext context,
            MultiSubTaskFactory subTaskFactory,
            Looper looper,
            TransferConfiguration configuration,
            ExecutorService taskExecutor) {
        super(context, looper, configuration, "Multi");
        mSubTaskFactory = subTaskFactory;
        mTaskExecutor = taskExecutor;
        mController = getController();
    }

    @Override
    public void onStart() {
        final ParcelFileDescriptor[] pipe;
        try {
            pipe = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            abortTask(e);
            return;
        }
        mReaderTask = mSubTaskFactory.getReaderSubTask(mController, pipe[1]);
        mWriterTask = mSubTaskFactory.getWriterSubTask(mController, pipe[0]);

        CompletableFuture.allOf(
                        execute(mTaskExecutor, mReaderTask),
                        execute(mTaskExecutor, mWriterTask))
                .thenAcceptAsync(v -> finishTask(), getExecutor())
                .exceptionally(
                        catching(
                                this::abortTask,
                                RemoteException.class,
                                InterruptedException.class,
                                IOException.class,
                                TimeoutException.class))
                .exceptionally(throwIn(getExecutor()));
    }
}
