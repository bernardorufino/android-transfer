package com.brufino.android.playground.components.main.pages.statistics;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import com.brufino.android.playground.transfer.TransferManager;

import java.util.concurrent.ExecutorService;

public class StatisticsViewModel extends AndroidViewModel {
    public StatisticsViewModel(
            Application application,
            TransferManager transferManager,
            ExecutorService workExecutor) {
        super(application);
    }
}
