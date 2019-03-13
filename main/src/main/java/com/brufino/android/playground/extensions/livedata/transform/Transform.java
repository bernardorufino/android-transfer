package com.brufino.android.playground.extensions.livedata.transform;

import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.brufino.android.common.utils.Preconditions.checkArgument;
import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.*;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class Transform<T> {
    public static <T> Transform<T> source(LiveData<T> source) {
        return new Transform<>(source);
    }

    private final LiveData<T> mSource;

    Transform(LiveData<T> source) {
        mSource = source;
    }

    protected LiveData<T> getSource() {
        return mSource;
    }

    public LiveData<T> getLiveData() {
        return getSource();
    }

    public Transform<T> mutate(Consumer<T> consumer, Executor executor) {
        MediatorLiveData<T> result = new MediatorLiveData<>();
        result.addSource(
                getSource(),
                new ConcurrentLatestObserver<>(
                        result,
                        (value, previousResult) -> {
                            consumer.accept(value);
                            return value;
                        },
                        result::setValue,
                        executor));
        return new Transform<>(result);
    }

    public <U> Transform<U> map(Function<T, U> function, Executor executor) {
        MediatorLiveData<U> result = new MediatorLiveData<>();
        result.addSource(
                getSource(),
                new ConcurrentLatestObserver<>(
                        result, (t, u) -> function.apply(t), result::setValue, executor));
        return new Transform<>(result);
    }

    public <U> Transform<U> map(Function<T, U> function) {
        return map(function, getMainThreadExecutor());
    }

    public <U extends T> Transform<U> castTo(Class<U> uClass, Executor executor) {
        return map(uClass::cast, executor);
    }

    public <U> Transform<U> switchMap(
            BiFunction<? super T, ? super U, ? extends LiveData<U>> function,
            Executor executor) {
        MediatorLiveData<U> result = new MediatorLiveData<>();
        result.addSource(
                getSource(),
                new ConcurrentLatestObserver<>(
                        result,
                        function,
                        new Consumer<LiveData<U>>() {
                            @Nullable private LiveData<U> mCurrentSource;

                            @Override
                            public void accept(@Nullable LiveData<U> liveData) {
                                if (mCurrentSource != null) {
                                    result.removeSource(mCurrentSource);
                                }
                                mCurrentSource = liveData;
                                if (mCurrentSource != null) {
                                    result.addSource(mCurrentSource, result::setValue);
                                }
                            }
                        },
                        executor));
        return new Transform<>(result);
    }

    public <U> Transform<U> switchMap(
            Function<? super T, ? extends LiveData<U>> function,
            Executor executor) {
        return switchMap((t, u) -> function.apply(t), executor);
    }

    public <U> Transform<U> switchMap(
            BiFunction<? super T, ? super U, ? extends LiveData<U>> function) {
        return switchMap(function, getMainThreadExecutor());
    }

    public <U> Transform<U> switchMap(
            Function<? super T, ? extends LiveData<U>> function) {
        return switchMap((t, u) -> function.apply(t), getMainThreadExecutor());
    }

    public <U, R> Transform<R> combine(
            LiveData<U> other,
            BiFunction<? super T, ? super U, ? extends R> function,
            Executor executor) {
        CombineLiveData<T, U, R> result =
                new CombineLiveData<>(getSource(), other, function, executor);
        return new Transform<>(result);
    }

    public <U, R> Transform<R> combine(
            LiveData<U> other,
            BiFunction<? super T, ? super U, ? extends R> function) {
        CombineLiveData<T, U, R> result =
                new CombineLiveData<>(getSource(), other, function);
        return new Transform<>(result);
    }

    public <U> OptionalTransform<U> optional() {
        //noinspection unchecked
        return new OptionalTransform<>((LiveData<Optional<U>>) getSource());
    }

    public Transform<T> update(long intervalMs) {
        MediatorLiveData<T> result = new MediatorLiveData<>();
        result.addSource(getSource(), result::setValue);
        postEvery(getMainThreadHandler(), () -> result.setValue(result.getValue()), intervalMs);
        return new Transform<>(result);
    }

    public Transform<List<T>> accumulateLast(int n) {
        MediatorLiveData<List<T>> result = new MediatorLiveData<>();
        result.addSource(getSource(), new Observer<T>() {
            private final List<T> mQueue = new LinkedList<>();

            @Override
            public void onChanged(T t) {
                if (mQueue.size() == n) {
                    mQueue.remove(0);
                }
                mQueue.add(t);
                result.postValue(new ArrayList<>(mQueue));
            }
        });
        return new Transform<>(result);
    }

    private static class CombineLiveData<T, U, R> extends MediatorLiveData<R> {
        private final BiFunction<? super T, ? super U, ? extends R> mFunction;
        @Nullable
        private final Executor mExecutor;
        private final Executor mMainExecutor;

        private boolean mTSet;
        private boolean mUSet;
        private boolean mValueSet;

        private @Nullable T mTValue;
        private @Nullable U mUValue;

        private CombineLiveData(
                LiveData<T> tLiveData,
                LiveData<U> uLiveData,
                BiFunction<? super T, ? super U, ? extends R> merge) {
            this(tLiveData, uLiveData, merge, null);
        }

        private CombineLiveData(
                LiveData<T> tLiveData,
                LiveData<U> uLiveData,
                BiFunction<? super T, ? super U, ? extends R> merge,
                @Nullable Executor executor) {
            checkArgument(tLiveData != uLiveData);
            mFunction = merge;
            mExecutor = executor;
            mMainExecutor = getMainThreadExecutor();
            addSource(checkNotNull(tLiveData), this::updateT);
            addSource(checkNotNull(uLiveData), this::updateU);
        }

        private void updateT(@Nullable T tValue) {
            mTSet = true;
            mTValue = tValue;
            update();
        }

        private void updateU(@Nullable U uValue) {
            mUSet = true;
            mUValue = uValue;
            update();
        }

        private void update() {
            if (mTSet && mUSet) {
                if (mExecutor != null) {
                    T tValue = mTValue;
                    U uValue = mUValue;
                    // TODO(brufino): Do same as ConcurrentLatestObserver with previous future.
                    CompletableFuture
                            .supplyAsync(() -> mFunction.apply(tValue, uValue), mExecutor)
                            .thenAcceptAsync(this::updateValue, mMainExecutor)
                            .exceptionally(throwIn(getMainThreadExecutor()));

                } else {
                    updateValue(mFunction.apply(mTValue, mUValue));
                }

            }
        }

        private void updateValue(R value) {
            if (!mValueSet || value != getValue()) {
                mValueSet = true;
                setValue(value);
            }
        }
    }

    /**
     * An {@link Observer} that executes the provided function in another thread and submits
     * the result in the main-thread, if multiple changes happen while the function is executing,
     * only the latest will be picked-up.
     */
    private static class ConcurrentLatestObserver<T, U, V> implements Observer<T> {
        private final MediatorLiveData<U> mLiveResult;
        private final BiFunction<? super T, ? super U, V> mFunction;
        private final Executor mMapExecutor;
        private final Consumer<? super V> mConsumer;
        private final Executor mMainExecutor;
        private final Object mLock = new Object();

        @GuardedBy("mLock")
        private CompletableFuture<Void> mPreviousFuture;

        @GuardedBy("mLock")
        private T mPending;

        @GuardedBy("mLock")
        private U mLastU;

        /**
         * Create a ConcurrentLatestObserver.
         *
         * @param liveResult The {@link MediatorLiveData} that will hold the result
         * @param function Function that's going to be called on one of {@code executor} threads.
         * @param consumer Consumer that's going to be called on main-thread.
         * @param executor Executor that provides threads to execute {@code function}.
         */
        private ConcurrentLatestObserver(
                MediatorLiveData<U> liveResult,
                BiFunction<? super T, ? super U, V> function,
                Consumer<? super V> consumer,
                Executor executor) {
            mLiveResult = liveResult;
            mFunction = function;
            mConsumer = consumer;
            mMapExecutor = executor;
            mMainExecutor = getMainThreadExecutor();
            mPreviousFuture = completedFuture(null);
        }

        @Override
        @MainThread
        public void onChanged(T t) {
            synchronized (mLock) {
                mLastU = mLiveResult.getValue();
                if (mPending != null) {
                    mPending = t;
                } else {
                    mPending = t;
                    // CompletableFuture won't hold reference to dependents after done.
                    mPreviousFuture =
                            mPreviousFuture
                                    .handleAsync((v, e) -> onMap(), mMapExecutor)
                                    .thenAcceptAsync(mConsumer, mMainExecutor)
                                    .exceptionally(throwIn(getMainThreadExecutor()));
                }
            }
        }

        private V onMap() {
            final T t;
            final U u;
            synchronized (mLock) {
                t = mPending;
                mPending = null;
                u = mLastU;
            }
            return mFunction.apply(t, u);
        }
    }
}
