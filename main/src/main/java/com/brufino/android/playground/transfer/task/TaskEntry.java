package com.brufino.android.playground.transfer.task;

import androidx.annotation.Nullable;
import com.brufino.android.playground.transfer.TransferCancelledException;
import com.brufino.android.playground.transfer.TransferConfiguration;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.brufino.android.playground.extensions.StringUtils.indent;
import static com.brufino.android.playground.extensions.ViewUtils.sizeString;

public class TaskEntry implements Serializable {
    private static final long serialVersionUID = 7075130700466722239L;

    public final String name;
    public final long duration;
    public final int inputRead;
    public final int outputWritten;
    public final TransferConfiguration configuration;
    @Nullable public final Map<String, TaskMeasurement> measurements;
    @Nullable public final Exception exception;

    TaskEntry(
            String name,
            long duration,
            int inputRead,
            int outputWritten,
            TransferConfiguration configuration,
            @Nullable Map<String, TaskMeasurement> measurements,
            @Nullable Exception exception) {
        this.name = name;
        this.duration = duration;
        this.inputRead = inputRead;
        this.outputWritten = outputWritten;
        this.configuration = configuration;
        this.measurements = measurements;
        this.exception = exception;
    }

    public boolean succeeded() {
        return exception == null;
    }

    public boolean cancelled() {
        return exception instanceof TransferCancelledException;
    }

    public String toMultilineString(int i) {
        return "TaskEntry{\n"
                + indent(i) + "name = '" + name + "\'\n"
                + indent(i) + "status = " + statusToString() + "\n"
                + indent(i) + "duration = " + duration + " ms\n"
                + indent(i) + "input read = " + sizeString(inputRead) + "\n"
                + indent(i) + "output written = " + sizeString(outputWritten) + "\n"
                + indent(i) + "configuration = " + configuration.toMultilineString(i + 1) + "\n"
                + indent(i) + "measurements = " + measurementsToMultilineString(i + 1) + "\n"
                + indent(i - 1) + "}";
    }

    private String statusToString() {
        if (exception == null) {
            return "succeeded";
        }
        return "failed (" + exception.getClass().getSimpleName() + ")";
    }

    private String measurementsToMultilineString(int i) {
        StringBuilder string = new StringBuilder("Map<String, TaskMeasurement>{\n");
        for (Map.Entry<String, TaskMeasurement> entry : measurements.entrySet()) {
            String label = entry.getKey();
            TaskMeasurement measurement = entry.getValue();
            string
                    .append(indent(i))
                    .append(label)
                    .append(" = ")
                    .append(measurementToMultilineString(i + 1, measurement))
                    .append("\n");
        }
        string.append(indent(i - 1)).append("}");
        return string.toString();
    }

    private String measurementToMultilineString(int i, TaskMeasurement measurement) {
        long oneMsNanos = TimeUnit.MILLISECONDS.toNanos(1);
        double average =  measurement.getAverage() / oneMsNanos;
        double sd = measurement.sd / oneMsNanos;
        double min = (double) measurement.min / oneMsNanos;
        double max = (double) measurement.max / oneMsNanos;
        double sum = (double) measurement.sum / oneMsNanos;
        return String.format(Locale.US, "%.1f ms (+- %.1f ms)", average, sd) + " {\n"
                + indent(i) + "min = " + String.format(Locale.US, "%.1f ms", min) + "\n"
                + indent(i) + "max = " + String.format(Locale.US, "%.1f ms", max) + "\n"
                + indent(i) + "count = " + measurement.count + "\n"
                + indent(i) + "sum = " + String.format(Locale.US, "%.1f ms", sum) + "\n"
                + indent(i - 1) + "}";
    }
}
