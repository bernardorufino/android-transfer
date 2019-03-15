package com.brufino.android.playground.extensions.livedata.transform;

import android.util.Pair;
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
import static com.brufino.android.common.utils.Preconditions.checkState;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.*;
import static java.util.concurrent.CompletableFuture.completedFuture;

/** These transformations are only interested in the latest value, not values in between. */
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
                new CombineLiveData<>(getSource(), other, function, getMainThreadExecutor());
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
        private final Executor mCombineExecutor;
        private final Executor mMainExecutor;

        private boolean mTSet;
        private boolean mUSet;
        private boolean mValueSet;
        private CompletableFuture<Void> mPreviousFuture;
        private Pair<T, U> mPending;

        private @Nullable T mLastT;
        private @Nullable U mLastU;

        private CombineLiveData(
                LiveData<T> liveT,
                LiveData<U> liveU,
                BiFunction<? super T, ? super U, ? extends R> merge,
                @Nullable Executor executor) {
            checkArgument(liveT != liveU);
            mFunction = merge;
            mCombineExecutor = executor;
            mMainExecutor = getMainThreadExecutor();
            mPreviousFuture = completedFuture(null);
            addSource(liveT, this::updateT);
            addSource(liveU, this::updateU);
        }

        @MainThread
        private void updateT(@Nullable T t) {
            mTSet = true;
            mLastT = t;
            update();
        }

        @MainThread
        private void updateU(@Nullable U u) {
            mUSet = true;
            mLastU = u;
            update();
        }

        @MainThread
        private void update() {
            checkState(isMainThread());
            if (!mTSet || !mUSet) {
                return;
            }
            if (mPending != null) {
                mPending = new Pair<>(mLastT, mLastU);
            } else {
                mPending = new Pair<>(mLastT, mLastU);
                mPreviousFuture
                        .handleAsync(
                                (v, e) -> {
                                    Pair<T, U> pending = mPending;
                                    mPending = null;
                                    return pending;
                                },
                                mMainExecutor)
                        .thenApplyAsync(
                                pair -> mFunction.apply(pair.first, pair.second), mCombineExecutor)
                        .thenAcceptAsync(this::updateValue, mMainExecutor)
                        .exceptionally(throwIn(mMainExecutor));
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
     * only the latest will be picked-up. It doesn't support {@code null} values.
     */
    private static class ConcurrentLatestObserver<T, U, V> implements Observer<T> {
        private final MediatorLiveData<U> mLiveResult;
        private final BiFunction<? super T, ? super U, V> mFunction;
        private final Executor mMapExecutor;
        private final Consumer<? super V> mConsumer;
        private final Executor mMainExecutor;
        private CompletableFuture<Void> mPreviousFuture;
        private T mPending;
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
        public void onChanged(T value) {
            mLastU = mLiveResult.getValue();
            if (mPending != null) {
                mPending = value;
            } else {
                mPending = value;
                // CompletableFuture won't hold reference to dependents after done.
                mPreviousFuture =
                        mPreviousFuture
                                .handleAsync(
                                        (v, e) -> {
                                            T t = mPending;
                                            U u = mLastU;
                                            mPending = null;
                                            return new Pair<>(t, u);
                                        },
                                        // On the main-thread so that we don't need to synchronize
                                        mMainExecutor)
                                .thenApplyAsync(this::onMap, mMapExecutor)
                                .thenAcceptAsync(mConsumer, mMainExecutor)
                                .exceptionally(throwIn(mMainExecutor));
            }

        }

        private V onMap(Pair<T, U> arguments) {
            T t = arguments.first;
            U u = arguments.second;
            return mFunction.apply(t, u);
        }
    }
}
