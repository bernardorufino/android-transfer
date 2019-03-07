package com.brufino.android.playground.components.main.pages.aggregate;

import androidx.annotation.MainThread;
import androidx.lifecycle.ViewModelProvider;
import com.brufino.android.playground.transfer.TransferManager;

public interface AggregateFragmentProvisioner {
    @MainThread
    TransferManager getTransferManager();

    ViewModelProvider.Factory getViewModelFactory();
}



