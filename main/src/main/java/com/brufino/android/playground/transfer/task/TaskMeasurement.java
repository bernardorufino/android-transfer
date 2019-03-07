package com.brufino.android.playground.transfer.task;

import java.io.Serializable;
import java.util.Collection;
import java.util.LongSummaryStatistics;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class TaskMeasurement implements Serializable {
    private static final long serialVersionUID = -374988166688949007L;

    static TaskMeasurement create(String label, Collection<? extends Long> times) {
        LongSummaryStatistics summary = times.stream().mapToLong(l -> l).summaryStatistics();
        double mean = summary.getAverage();
        double sd =
                sqrt(
                        times
                                .stream()
                                .map(time -> pow(time - mean, 2))
                                .mapToDouble(d -> d)
                                .average()
                                .orElse(.0d));
        return new TaskMeasurement(
                label,
                summary.getCount(),
                summary.getSum(),
                summary.getMin(),
                summary.getMax(),
                sd);
    }

    public final String label;
    public final long count;
    public final long sum;
    public final long min;
    public final long max;
    public final double sd;

    private TaskMeasurement(String label, long count, long sum, long min, long max, double sd) {
        this.label = label;
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
        this.sd = sd;
    }

    public double getAverage() {
        return count > 0 ? (double) sum / count : 0d;
    }
}
