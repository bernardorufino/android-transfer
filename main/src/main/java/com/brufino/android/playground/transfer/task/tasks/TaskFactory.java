package com.brufino.android.playground.transfer.task.tasks;

import android.os.Looper;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.extensions.service.ServiceClientFactory;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.transfer.task.TransferTask;
import com.brufino.android.playground.transfer.task.tasks.multi.MultiThreadTask;
import com.brufino.android.playground.transfer.task.tasks.multi.subtasks.MultiSubTaskFactory;
import com.brufino.android.playground.transfer.task.tasks.single.SingleThreadTask;

import java.util.concurrent.ExecutorService;

public class TaskFactory {
    private final ApplicationContext mContext;
    private final ServiceClientFactory mClientFactory;
    private final MultiSubTaskFactory mMultiSubTaskFactory;
    private final ExecutorService mTaskReaderExecutor;
    private final ExecutorService mTaskWriterExecutor;

    public TaskFactory(
            ApplicationContext context,
            ServiceClientFactory clientFactory,
            MultiSubTaskFactory multiSubTaskFactory,
            ExecutorService taskReaderExecutor,
            ExecutorService taskWriterExecutor) {
        mContext = context;
        mClientFactory = clientFactory;
        mMultiSubTaskFactory = multiSubTaskFactory;
        mTaskReaderExecutor = taskReaderExecutor;
        mTaskWriterExecutor = taskWriterExecutor;
    }

    public TransferTask getTask(
            @TransferManager.Code int code,
            TransferConfiguration configuration,
            Looper looper) {
        switch (code) {
            case TransferManager.Code.SINGLE_THREAD:
                return new SingleThreadTask(mContext, mClientFactory, looper, configuration);
            case TransferManager.Code.MULTI_THREAD:
                return new MultiThreadTask(
                        mContext,
                        mMultiSubTaskFactory,
                        looper,
                        configuration,
                        mTaskReaderExecutor,
                        mTaskWriterExecutor);
            default:
                throw new IllegalArgumentException("Unknown task code " + code);
        }
    }
}
