package com.brufino.android.playground.transfer.task;

import com.brufino.android.playground.transfer.TransferConfiguration;

import java.util.concurrent.TimeUnit;

import static com.brufino.android.common.utils.Preconditions.checkState;

public class TaskInformation {
    private static final int END_TIME_NOT_SET = -1;

    public static TaskInformation started(String name, TransferConfiguration configuration) {
        return new TaskInformation(name, System.nanoTime(), END_TIME_NOT_SET, 0, 0, configuration);
    }

    private final long mStartTimeNano;
    private final long mEndTimeNano;
    public final String name;
    public final int inputRead;
    public final int outputWritten;
    public final TransferConfiguration configuration;

    private TaskInformation(
            String name,
            long startTimeNano,
            long endTimeNano,
            int inputRead,
            int outputWritten,
            TransferConfiguration configuration) {
        this.name = name;
        this.mStartTimeNano = startTimeNano;
        this.mEndTimeNano = endTimeNano;
        this.inputRead = inputRead;
        this.outputWritten = outputWritten;
        this.configuration = configuration;
    }

    /** In milliseconds. */
    public long getDuration() {
        checkState(mEndTimeNano != END_TIME_NOT_SET, "TaskInformation not ended.");
        return TimeUnit.NANOSECONDS.toMillis(mEndTimeNano - mStartTimeNano);
    }

    /** In milliseconds. */
    public long getDurationOrTimeElapsed() {
        long interval =
                mEndTimeNano != END_TIME_NOT_SET
                        ? getDuration()
                        : System.nanoTime() - mStartTimeNano;
        return TimeUnit.NANOSECONDS.toMillis(interval);
    }

    TaskInformation setEnded() {
        checkState(mEndTimeNano == END_TIME_NOT_SET, "TaskInformation already marked as ended.");
        return new TaskInformation(
                name,
                mStartTimeNano,
                System.nanoTime(),
                inputRead,
                outputWritten,
                configuration);
    }

    TaskInformation addInputRead(int value) {
        return new TaskInformation(
                name,
                mStartTimeNano,
                mEndTimeNano,
                inputRead + value,
                outputWritten,
                configuration);
    }

    TaskInformation addOutputWritten(int value) {
        return new TaskInformation(
                name,
                mStartTimeNano,
                mEndTimeNano,
                inputRead,
                outputWritten + value,
                configuration);
    }

    TaskInformation setConfiguration(TransferConfiguration value) {
        return new TaskInformation(
                name,
                mStartTimeNano,
                mEndTimeNano,
                inputRead,
                outputWritten,
                value);
    }
}
