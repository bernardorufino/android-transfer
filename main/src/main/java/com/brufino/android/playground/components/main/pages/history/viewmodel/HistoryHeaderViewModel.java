package com.brufino.android.playground.components.main.pages.history.viewmodel;

import com.brufino.android.playground.R;

public class HistoryHeaderViewModel extends HistoryItemViewModel {
    public static HistoryHeaderViewModel empty() {
        return new HistoryHeaderViewModel(0, 0, 0);
    }

    public final long totalTasks;
    public final long successfulTasks;
    public final long failedTasks;

    HistoryHeaderViewModel(long totalTasks, long successfulTasks, long failedTasks) {
        super(R.layout.history_header);
        this.totalTasks = totalTasks;
        this.successfulTasks = successfulTasks;
        this.failedTasks = failedTasks;
    }

    public String getText() {
        return totalTasks
                + " tasks ("
                + successfulTasks
                + " succeeded)";
    }
}
