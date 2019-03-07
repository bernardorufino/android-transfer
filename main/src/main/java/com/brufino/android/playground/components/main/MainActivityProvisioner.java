package com.brufino.android.playground.components.main;

import androidx.annotation.MainThread;
import androidx.lifecycle.ViewModelProvider;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.extensions.permission.PermissionRequester;

import java.util.concurrent.ExecutorService;

public interface MainActivityProvisioner {
    PermissionRequester getPermissionRequester();

    @MainThread
    TransferManager getTransferManager();

    ExecutorService getWorkExecutor();

    ViewModelProvider.Factory getViewModelFactory();

    TaskSheet getTaskSheet();
}
