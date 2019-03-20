package com.brufino.android.playground.components.main.pages.history.viewmodel;

import android.view.View;

public class HistoryEntryViewModel {
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
            View.OnClickListener onTaskClicked,
            View.OnClickListener onTaskErrorClicked) {
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
        this.onTaskClicked = onTaskClicked;
        this.onTaskErrorClicked = onTaskErrorClicked;
    }
}
