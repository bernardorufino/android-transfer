package com.brufino.android.playground.extensions.livedata;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

public class Repository<T> extends LiveData<T> {

    @Override
    protected void postValue(T value) {
        super.postValue(value);
    }

    @Override
    protected void setValue(T value) {
        super.setValue(value);
    }

    @Nullable
    @Override
    public T getValue() {
        return super.getValue();
    }

    @Override
    protected void onActive() {
        super.onActive();
    }

    @Override
    protected void onInactive() {
        super.onInactive();
    }
}
