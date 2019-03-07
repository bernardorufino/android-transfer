package com.brufino.android.playground.transfer.service;

import com.brufino.android.playground.transfer.task.TaskManager;

public interface TransferServiceProvisioner {
    TaskManager getTaskManager();
}
