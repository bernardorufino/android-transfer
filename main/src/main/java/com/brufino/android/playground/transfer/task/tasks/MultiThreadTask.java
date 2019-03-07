package com.brufino.android.playground.transfer.task.tasks;

import android.os.Looper;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.transfer.task.TransferTask;
import com.brufino.android.playground.extensions.ApplicationContext;

public class MultiThreadTask extends TransferTask {
    MultiThreadTask(
            ApplicationContext context,
            Looper looper,
            TransferConfiguration configuration) {
        super(context, looper, configuration, "Multi");
    }

    @Override
    public void onStart() {
        finishTask();
    }
}
