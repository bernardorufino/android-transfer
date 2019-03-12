package com.brufino.android.playground.extensions;

public interface ThrowingFunction<I, O, E extends Throwable> {
    O apply(I input) throws E;
}
