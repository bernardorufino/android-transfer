package com.brufino.android.playground.transfer;

import java.util.concurrent.ExecutorService;

public interface TransferManagerServiceProvisioner {
    TransferManager getTransferManager();
    ExecutorService getRequestExecutor();
}
