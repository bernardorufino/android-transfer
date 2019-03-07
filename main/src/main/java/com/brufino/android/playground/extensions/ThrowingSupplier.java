package com.brufino.android.playground.extensions;

public interface ThrowingSupplier<T, E extends Throwable> {
    T get() throws E;
}
