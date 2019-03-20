package com.brufino.android.playground.transfer.task;

import androidx.annotation.Nullable;
import com.brufino.android.playground.transfer.TransferConfiguration;

import java.util.concurrent.TimeUnit;

import static com.brufino.android.common.utils.Preconditions.checkState;

public class TaskInformation {
    private static final int END_TIME_NOT_SET = -1;

    static TaskInformation started(String name, TransferConfiguration configuration) {
        return new TaskInformation(
                name, System.nanoTime(), END_TIME_NOT_SET, 0, 0, configuration, null);
    }

    private final long mStartTimeNano;
    private final long mEndTimeNano;
    public final String name;
    public final int inputRead;
    public final int outputWritten;
    public final TransferConfiguration configuration;
    @Nullable public final Exception exception;


    private TaskInformation(
            String name,
            long startTimeNano,
            long endTimeNano,
            int inputRead,
            int outputWritten,
            TransferConfiguration configuration,
            @Nullable Exception exception) {
        this.name = name;
        this.mStartTimeNano = startTimeNano;
        this.mEndTimeNano = endTimeNano;
        this.inputRead = inputRead;
        this.outputWritten = outputWritten;
        this.configuration = configuration;
        this.exception = exception;
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

    TaskInformation setEnded(@Nullable Exception exception) {
        checkState(mEndTimeNano == END_TIME_NOT_SET, "TaskInformation already marked as ended.");
        return new TaskInformation(
                name,
                mStartTimeNano,
                System.nanoTime(),
                inputRead,
                outputWritten,
                configuration,
                exception);
    }

    TaskInformation addInputRead(int value) {
        return new TaskInformation(
                name,
                mStartTimeNano,
                mEndTimeNano,
                inputRead + value,
                outputWritten,
                configuration,
                exception);
    }

    TaskInformation addOutputWritten(int value) {
        return new TaskInformation(
                name,
                mStartTimeNano,
                mEndTimeNano,
                inputRead,
                outputWritten + value,
                configuration,
                exception);
    }

    TaskInformation setConfiguration(TransferConfiguration value) {
        return new TaskInformation(
                name,
                mStartTimeNano,
                mEndTimeNano,
                inputRead,
                outputWritten,
                value,
                exception);
    }
}
