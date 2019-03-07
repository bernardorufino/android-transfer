package com.brufino.android.playground.extensions.livedata;

import androidx.annotation.Nullable;
import androidx.lifecycle.ComputableLiveData;
import androidx.lifecycle.LiveData;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static com.brufino.android.playground.extensions.livedata.LiveDataUtils.computableLiveData;

/**
 * Any value set with {@link ImmediateLiveData#setValue(Object)} will be immediately visible to
 * other threads with {@link ImmediateLiveData#getValue()}.
 */
public class ImmediateLiveData<T> {
    private final AtomicReference<T> mReference;
    private final ComputableLiveData<T> mLiveData;

    public ImmediateLiveData(Executor executor) {
        mReference = new AtomicReference<>();
        mLiveData = computableLiveData(executor, mReference::get);
    }

    public ImmediateLiveData(T value, Executor executor) {
        mReference = new AtomicReference<>(value);
        mLiveData = computableLiveData(executor, mReference::get);
    }

    public LiveData<T> getLiveData() {
        return mLiveData.getLiveData();
    }

    @Nullable
    public T getValue() {
        return mReference.get();
    }

    public void setValue(T value) {
        mReference.set(value);
        mLiveData.invalidate();
    }

    public void updateValue(UnaryOperator<T> operator) {
        mReference.updateAndGet(operator);
        mLiveData.invalidate();
    }
}
