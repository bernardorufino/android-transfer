package com.brufino.android.playground.components.main.pages.history.viewmodel;

import android.view.View;
import com.brufino.android.playground.R;

public class HistoryEntryViewModel extends HistoryItemViewModel {
    public final int cellBackgroundColor;
    public final String task;
    public final String speed;
    public final String outputWrittenAndDuration;
    public final int outputWrittenAndDurationBackground;
    public final String producerInterval;
    public final String producerChunkSize;
    public final String transferBufferSize;
    public final String consumerInterval;
    public final String consumerBufferSize;
    public final boolean errorVisible;
    public final View.OnClickListener onTaskClicked;
    public final View.OnClickListener onTaskErrorClicked;
    public final String errorText;

    HistoryEntryViewModel(
            int cellBackgroundColor,
            String task,
            String speed,
            String outputWrittenAndDuration,
            int outputWrittenAndDurationBackground,
            String producerInterval,
            String producerChunkSize,
            String transferBufferSize,
            String consumerInterval,
            String consumerBufferSize,
            boolean errorVisible,
            String errorText,
            View.OnClickListener onTaskClicked,
            View.OnClickListener onTaskErrorClicked) {
        super(R.layout.history_entry);
        this.cellBackgroundColor = cellBackgroundColor;
        this.task = task;
        this.speed = speed;
        this.outputWrittenAndDuration = outputWrittenAndDuration;
        this.outputWrittenAndDurationBackground = outputWrittenAndDurationBackground;
        this.producerInterval = producerInterval;
        this.producerChunkSize = producerChunkSize;
        this.transferBufferSize = transferBufferSize;
        this.consumerInterval = consumerInterval;
        this.consumerBufferSize = consumerBufferSize;
        this.errorVisible = errorVisible;
        this.errorText = errorText;
        this.onTaskClicked = onTaskClicked;
        this.onTaskErrorClicked = onTaskErrorClicked;
    }
}
