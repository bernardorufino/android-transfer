package com.brufino.android.playground.transfer.task.tasks;

import android.os.Looper;
import com.brufino.android.playground.extensions.service.ServiceClientFactory;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.transfer.task.TransferTask;
import com.brufino.android.playground.extensions.ApplicationContext;

public class TaskFactory {
    private final ApplicationContext mContext;
    private final ServiceClientFactory mClientFactory;

    public TaskFactory(
            ApplicationContext context,
            ServiceClientFactory clientFactory) {
        mContext = context;
        mClientFactory = clientFactory;
    }

    public TransferTask getTask(
            @TransferManager.Code int code,
            TransferConfiguration configuration,
            Looper looper) {
        switch (code) {
            case TransferManager.Code.SINGLE_THREAD:
                return new SingleThreadTask(mContext, mClientFactory, looper, configuration);
            case TransferManager.Code.MULTI_THREAD:
                return new MultiThreadTask(mContext, looper, configuration);
            default:
                throw new IllegalArgumentException("Unknown task code " + code);
        }
    }
}
