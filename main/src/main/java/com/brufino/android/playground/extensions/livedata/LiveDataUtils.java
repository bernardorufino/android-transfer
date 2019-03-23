package com.brufino.android.playground.extensions.livedata;

import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.lifecycle.ComputableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
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
public class LiveDataUtils {
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

    public static <T> void futureLiveData(
            MutableLiveData<CompletableFuture<T>> liveData, CompletableFuture<T> completableFuture) {
        setValue(liveData, completableFuture);
        completableFuture.whenComplete((value, error) -> setValue(liveData, completableFuture));
        liveData.observeForever(
                future -> {
                    if (future != null && future.isDone()) {
                        // Reset future
                        liveData.postValue(null);
                    }
                });
    }

    public static <T> boolean isLoading(@Nullable CompletableFuture<T> future) {
        return future != null && !future.isDone();
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