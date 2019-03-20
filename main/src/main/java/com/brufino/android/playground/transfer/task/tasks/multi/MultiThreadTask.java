package com.brufino.android.playground.transfer.task.tasks.multi;

import android.os.Looper;
import android.os.ParcelFileDescriptor;
import com.brufino.android.playground.extensions.ThrowingRunnable;
import com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils;
import com.brufino.android.playground.extensions.concurrent.HandlerExecutor;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.transfer.task.TaskController;
import com.brufino.android.playground.transfer.task.TransferTask;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.transfer.task.tasks.multi.subtasks.ProducerReaderSubTask;
import com.brufino.android.playground.transfer.task.tasks.multi.subtasks.MultiSubTaskFactory;
import com.brufino.android.playground.transfer.task.tasks.multi.subtasks.ConsumerWriterSubTask;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static android.os.Looper.myLooper;
import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.execute;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.getMainThreadExecutor;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.throwIn;

public class MultiThreadTask extends TransferTask {
    private final MultiSubTaskFactory mSubTaskFactory;
    private final TaskController mController;
    private final ExecutorService mReaderExecutor;
    private final ExecutorService mWriterExecutor;
    private ProducerReaderSubTask mReaderTask;
    private ConsumerWriterSubTask mWriterTask;

    public MultiThreadTask(
            ApplicationContext context,
            MultiSubTaskFactory subTaskFactory,
            Looper looper,
            TransferConfiguration configuration,
            ExecutorService taskReaderExecutor,
            ExecutorService taskWriterExecutor) {
        super(context, looper, configuration, "Multi");
        mSubTaskFactory = subTaskFactory;
        mReaderExecutor = taskReaderExecutor;
        mWriterExecutor = taskWriterExecutor;
        mController = getController();
    }

    @Override
    public void onStart() {
        final ParcelFileDescriptor[] pipe;
        try {
            pipe = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            // TODO(brufino): Don't crash the process
            throw new RuntimeException(e);
        }
        mReaderTask = mSubTaskFactory.getReaderSubTask(mController, pipe[1]);
        mWriterTask = mSubTaskFactory.getWriterSubTask(mController, pipe[0]);

        CompletableFuture.allOf(
                        execute(mReaderExecutor, mReaderTask),
                        execute(mWriterExecutor, mWriterTask))
                .thenAcceptAsync(v -> finishTask(), getExecutor())
                .exceptionally(throwIn(getExecutor()));
    }

}
