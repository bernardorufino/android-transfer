package com.brufino.android.playground.extensions;

public interface ThrowingRunnable<E extends Throwable> {
    void run() throws E;
}
