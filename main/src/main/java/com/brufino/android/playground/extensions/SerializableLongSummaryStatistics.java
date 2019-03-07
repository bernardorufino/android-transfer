package com.brufino.android.playground.extensions;

import androidx.annotation.NonNull;

import java.util.LongSummaryStatistics;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public class SerializableLongSummaryStatistics implements LongConsumer, IntConsumer {
    private final LongSummaryStatistics mSummary;

    public SerializableLongSummaryStatistics(LongSummaryStatistics summary) {
        mSummary = summary;
    }

    public LongSummaryStatistics getSummary() {
        return mSummary;
    }

    @Override
    public void accept(int value) {
        mSummary.accept(value);
    }

    @Override
    public void accept(long value) {
        mSummary.accept(value);
    }

    public void combine(LongSummaryStatistics other) {
        mSummary.combine(other);
    }

    public void combine(SerializableLongSummaryStatistics other) {
        mSummary.combine(other.mSummary);
    }

    public long getCount() {
        return mSummary.getCount();
    }

    public long getSum() {
        return mSummary.getSum();
    }

    public long getMin() {
        return mSummary.getMin();
    }

    public long getMax() {
        return mSummary.getMax();
    }

    public double getAverage() {
        return mSummary.getAverage();
    }

    @NonNull
    @Override
    public String toString() {
        return mSummary.toString();
    }
}
