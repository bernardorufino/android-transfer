package com.brufino.android.playground.components.command;

import android.content.Context;
import com.brufino.android.playground.transfer.TransferManager;

import java.util.concurrent.ExecutorService;

public interface CommandReceiverProvisioner {
    TransferManager getTransferManager(Context context);
    ExecutorService getWorkExecutor();
    ExecutorService getRequestExecutor();
}
