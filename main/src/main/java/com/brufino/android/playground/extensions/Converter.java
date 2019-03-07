package com.brufino.android.playground.extensions;

import java.util.function.Function;

public interface Converter<A, B> extends Function<A, B> {
    static <T> Converter<T, T> identity() {
        return from(Function.identity(), Function.identity());
    }

    static <A, B> Converter<A, B> from(Function<A, B> forward, Function<B, A> backward) {
        return new Converter<A, B>() {
            @Override
            public B convert(A a) {
                return forward.apply(a);
            }
            @Override
            public A revert(B b) {
                return backward.apply(b);
            }
        };
    }

    B convert(A a);

    A revert(B b);

    default Converter<B, A> invert() {
        return from(this::revert, this::convert);
    }

    @Override
    default B apply(A a) {
        return convert(a);
    }
}
