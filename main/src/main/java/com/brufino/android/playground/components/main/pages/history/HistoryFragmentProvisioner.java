package com.brufino.android.playground.components.main.pages.history;

import androidx.annotation.MainThread;
import androidx.lifecycle.ViewModelProvider;
import com.brufino.android.playground.transfer.TransferManager;

public interface HistoryFragmentProvisioner {
    @MainThread
    TransferManager getTransferManager();

    ViewModelProvider.Factory getViewModelFactory();

    HistoryAdapter getHistoryAdapter();
}
