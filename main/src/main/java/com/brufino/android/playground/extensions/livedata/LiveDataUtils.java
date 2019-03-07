package com.brufino.android.playground.extensions.livedata;

import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.lifecycle.ComputableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.postOnMainThread;

/**
 * Utility methods for using {@link LiveData}. In general for Boolean operations, {@code null} is
 * treated as an "unknown" value, and operations may use short-circuit evaluation to determine the
 * result. LiveData may be in an uninitialized state where observers are not called when registered
 * (e.g. a {@link MutableLiveData} where {@link MutableLiveData#setValue(Object)} has not yet been
 * called). If a Boolean operation receives an uninitialized LiveData as either of its parameters,
 * the result will also be in an uninitialized state.
 */
// TODO(brufino): Do it myself :P
public class LiveDataUtils {
    private static volatile LiveData<?> nullLiveData;
    private static volatile LiveData<Boolean> trueLiveData;
    private static volatile LiveData<Boolean> falseLiveData;

    /**
     * Returns a LiveData that always emits {@code null}. This is different than an uninitialized
     * LiveData in that observers will be called (with {@code null}) when registered.
     */
    public static <T> LiveData<T> nullLiveData() {
        if (nullLiveData == null) {
            MutableLiveData<?> nullData = new MutableLiveData<>();
            nullData.setValue(null);
            nullLiveData = nullData;
        }
        // null can fit any generic type
        // noinspection unchecked
        return (LiveData<T>) nullLiveData;
    }

    /** Returns a LiveData that always emits {@code true}. */
    public static LiveData<Boolean> trueLiveData() {
        if (trueLiveData == null) {
            MutableLiveData<Boolean> trueData = new MutableLiveData<>();
            trueData.setValue(true);
            trueLiveData = trueData;
        }
        return trueLiveData;
    }

    /** Returns a LiveData that always emits {@code false}. */
    public static LiveData<Boolean> falseLiveData() {
        if (falseLiveData == null) {
            MutableLiveData<Boolean> falseData = new MutableLiveData<>();
            falseData.setValue(false);
            falseLiveData = falseData;
        }
        return falseLiveData;
    }

    /** Returns a LiveData that always emits {@code value}. */
    public static <T> LiveData<T> constantLiveData(@Nullable T value) {
        MutableLiveData<T> liveData = new MutableLiveData<>();
        setValue(liveData, value);
        return liveData;
    }

    /** Returns a LiveData that always emits {@code value} if it's not null. */
    public static <T> LiveData<T> constantLiveDataIfNotNull(@Nullable T value) {
        MutableLiveData<T> liveData = new MutableLiveData<>();
        if (value != null) {
            setValue(liveData, value);
        }
        return liveData;
    }

    /**
     * Returns a LiveData that is backed by {@code trueData} when the trigger satisfies the predicate,
     * {@code falseData} when the trigger does not satisfy the predicate, and emits {@code null} when
     * the trigger emits {@code null}.
     *
     * @param trueData the LiveData whose value should be emitted when predicate returns {@code true}
     * @param falseData the LiveData whose value should be emitted when predicate returns {@code
     *     false}
     */
    public static <P, T> LiveData<T> ifThenElse(
            LiveData<P> trigger,
            Predicate<? super P> predicate,
            LiveData<T> trueData,
            LiveData<T> falseData) {
        return Transformations.switchMap(
                trigger,
                t -> {
                    if (t == null) {
                        return nullLiveData();
                    } else {
                        return predicate.test(t) ? trueData : falseData;
                    }
                });
    }

    public static <T> ComputableLiveData<T> computableLiveData(Supplier<T> function) {
        return new ComputableLiveData<T>() {
            @Override
            protected T compute() {
                return function.get();
            }
        };
    }

    public static <T> ComputableLiveData<T> computableLiveData(
            Executor executor, Supplier<T> function) {
        return new ComputableLiveData<T>(executor) {
            @Override
            protected T compute() {
                return function.get();
            }
        };
    }

    public static <T> androidx.arch.core.util.Function<T, T> identity() {
        return input -> input;
    }

    public static <T> void futureAsync(
            MutableLiveData<CompletableFuture<T>> liveData, CompletableFuture<T> future) {
        setValue(liveData, future);
        future.whenComplete((value, error) -> setValue(liveData, future));
    }

    public static <T> void setValue(MutableLiveData<T> liveData, T value) {
        if (ConcurrencyUtils.isMainThread()) {
            liveData.setValue(value);
            return;
        }
        ConditionVariable valueSet = new ConditionVariable(false);
        postOnMainThread(() -> {
            liveData.setValue(value);
            valueSet.open();
        });
        valueSet.block();
    }

    public static <T> MutableLiveData<T> mutableLiveData(T initialValue) {
        MutableLiveData<T> liveData = new MutableLiveData<>();
        setValue(liveData, initialValue);
        return liveData;
    }

    /**
     * Call this when you want to resume execution only when the given {@code looper} has
     * finished all work that it had when you called the method.
     */
    public static void waitLooper(Looper looper) {
        if (looper.getQueue().isIdle()) {
            return;
        }
        ConditionVariable reached = new ConditionVariable(false);
        new Handler(looper).post(reached::open);
        reached.block();
    }

    /**
     * Waits until all queued operations on {@code liveData} are executed to return its final
     * value.
     */
    public static <T> T getLatestValue(LiveData<T> liveData) {
        waitLooper(Looper.getMainLooper());
        return liveData.getValue();
    }

    public static <T> void postUpdateValue(MutableLiveData<T> liveData, Function<T, T> update) {
        postOnMainThread(() -> liveData.setValue(update.apply(liveData.getValue())));
    }

    private LiveDataUtils() {}

}