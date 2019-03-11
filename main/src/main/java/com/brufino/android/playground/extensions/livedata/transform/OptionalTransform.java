package com.brufino.android.playground.extensions.livedata.transform;

import androidx.lifecycle.LiveData;
import com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils;
import com.brufino.android.playground.extensions.livedata.LiveDataUtils;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.getMainThreadExecutor;
import static com.brufino.android.playground.extensions.livedata.LiveDataUtils.constantLiveData;
import static java.util.Optional.empty;

public class OptionalTransform<T> extends Transform<Optional<T>> {
    OptionalTransform(LiveData<Optional<T>> source) {
        super(source);
    }

    public <U extends T> OptionalTransform<U> castToOptionalOf(Class<U> uClass, Executor executor) {
        return mapIfPresent(uClass::cast, executor);
    }

    public <U> OptionalTransform<U> mapIfPresent(
            Function<? super T, ? extends U> function, Executor executor) {
        return map(optional -> optional.map(function), executor).optional();
    }

    public <U> OptionalTransform<U> mapIfPresent(Function<? super T, ? extends U> function) {
        return map(optional -> optional.map(function)).optional();
    }

    public <U> OptionalTransform<U> switchMapIfPresent(
            Function<? super T, LiveData<U>> function) {
        return switchMapIfPresent(function, getMainThreadExecutor());
    }

    public <U> OptionalTransform<U> switchMapIfPresent(
            Function<? super T, LiveData<U>> function, Executor executor) {
        return switchMap(
                        optional ->
                                optional
                                        .map(
                                                value ->
                                                        new Transform<>(function.apply(value))
                                                                .map(Optional::of)
                                                                .getLiveData())
                                        .orElse(constantLiveData(empty())),
                        executor)
                .optional();
    }

    public Transform<T> orElse(T value, Executor executor) {
        return map(optional -> optional.orElse(value), executor);
    }

    public Transform<T> orElse(T value) {
        return map(optional -> optional.orElse(value));
    }
}
