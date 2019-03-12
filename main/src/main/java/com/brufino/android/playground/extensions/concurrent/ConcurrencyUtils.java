package com.brufino.android.playground.extensions.concurrent;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.brufino.android.playground.extensions.ThrowingFunction;
import com.brufino.android.playground.extensions.ThrowingRunnable;
import com.brufino.android.playground.extensions.ThrowingSupplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConcurrencyUtils {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final HandlerExecutor MAIN_EXECUTOR = new HandlerExecutor(MAIN_HANDLER);

    public static void postEvery(Handler handler, Runnable runnable, long intervalMs) {
        handler.postDelayed(new LoopingRunnable(handler, runnable, intervalMs), intervalMs);
    }

    public static boolean isMainThread() {
        return Looper.getMainLooper().isCurrentThread();
    }

    public static void postOnMainThread(Runnable runnable) {
        MAIN_HANDLER.post(runnable);
    }

    public static Executor getMainThreadExecutor() {
        return MAIN_EXECUTOR;
    }

    public static Handler getMainThreadHandler() {
        return MAIN_HANDLER;
    }

    public static <T> CompletableFuture<T> execute(
            Executor executor, ThrowingSupplier<T, Exception> supplier) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return supplier.get();
                    } catch (Exception e) {
                        throw getCompletionException(e);
                    }
                },
                executor);
    }

    public static CompletableFuture<Void> execute(
            Executor executor, ThrowingRunnable<Throwable> runnable) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        runnable.run();
                        return null;
                    } catch (Throwable e) {
                        throw getCompletionException(e);
                    }
                },
                executor);
    }

    public static void asyncThrowing(ThrowingRunnable<Exception> runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw getCompletionException(e);
        }
    }

    public static <T> T asyncThrowing(ThrowingSupplier<T, Exception> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw getCompletionException(e);
        }
    }

    public static <I, O> Function<I, O> asyncThrowing(ThrowingFunction<I, O, Exception> function) {
        return (I input) -> {
            try {
                return function.apply(input);
            } catch (Exception e) {
                throw getCompletionException(e);
            }
        };

    }

    public static <T> Function<Throwable, T> throwIn(Executor executor) {
        return throwable -> {
            CompletionException exception = getCompletionException(throwable);
            if (!(exception.getCause() instanceof ExceptionCaught)) {
                executor.execute(
                        () -> {
                            throw exception;
                        });
            }
            throw exception;
        };
    }

    public static <T, E> Function<Throwable, T> catching(
            Class<E> exceptionClass, Consumer<E> consumer) {
        return throwable -> {
            if (exceptionClass.isInstance(throwable)) {
                consumer.accept(exceptionClass.cast(throwable));
                throw new ExceptionCaught(throwable);
            } else if (throwable instanceof CompletionException) {
                Throwable cause = throwable.getCause();
                if (exceptionClass.isInstance(cause)) {
                    consumer.accept(exceptionClass.cast(cause));
                    throw new ExceptionCaught(cause);
                }
            }
            throw getCompletionException(throwable);
        };

    }

    public static void resettingInterrupt(InterruptibleRunnable runnable) {
        try {
            runnable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private static CompletionException getCompletionException(Throwable throwable) {
        if (throwable instanceof CompletionException) {
            return (CompletionException) throwable;
        }
        return new CompletionException(throwable);
    }

    private static class ExceptionCaught extends RuntimeException {
        private ExceptionCaught(Throwable throwable) {
            super(throwable);
        }
    }
}
