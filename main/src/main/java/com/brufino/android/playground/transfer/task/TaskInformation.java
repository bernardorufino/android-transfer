package com.brufino.android.playground.transfer.task;

import com.brufino.android.playground.transfer.TransferConfiguration;

public class TaskInformation {
    public final String name;
    public final long startTime;
    public final int inputRead;
    public final int outputWritten;
    public final TransferConfiguration configuration;

    TaskInformation(
            String name,
            long startTime,
            int inputRead,
            int outputWritten,
            TransferConfiguration configuration) {
        this.name = name;
        this.startTime = startTime;
        this.inputRead = inputRead;
        this.outputWritten = outputWritten;
        this.configuration = configuration;
    }

    public long getTimeElapsed() {
        // TODO(brufino): Use System.nanoTime()
        return System.currentTimeMillis() - startTime;
    }

    public TaskInformation setStartTime(long value) {
        return new TaskInformation(
                name,
                value,
                inputRead,
                outputWritten,
                configuration);
    }

    public TaskInformation addInputRead(int value) {
        return new TaskInformation(
                name,
                startTime,
                inputRead + value,
                outputWritten,
                configuration);
    }

    public TaskInformation addOutputWritten(int value) {
        return new TaskInformation(
                name,
                startTime,
                inputRead,
                outputWritten + value,
                configuration);
    }

    public TaskInformation setConfiguration(TransferConfiguration value) {
        return new TaskInformation(
                name,
                startTime,
                inputRead,
                outputWritten,
                value);
    }
}
