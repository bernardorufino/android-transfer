package com.brufino.android.playground.extensions.concurrent;

import android.os.Handler;
import android.os.Looper;
import com.brufino.android.playground.extensions.ThrowingRunnable;
import com.brufino.android.playground.extensions.ThrowingSupplier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
            ThrowingSupplier<T, ? extends Exception> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(() -> asyncThrows(supplier), executor);
    }

    public static CompletableFuture<Void> execute(
            ThrowingRunnable<? extends Exception> runnable, Executor executor) {
        return CompletableFuture.supplyAsync(
                () -> {
                    asyncThrows(runnable);
                    return null;
                },
                executor);
    }

    public static void asyncThrows(ThrowingRunnable<? extends Exception> runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw getCompletionException(e);
        }
    }

    public static <T> T asyncThrows(ThrowingSupplier<T, ? extends Exception> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw getCompletionException(e);
        }
    }

    public static <I, O> Function<I, O> asyncThrowingFunction(
            Supplier<? extends Exception> exceptionSupplier) {
        return (I input) -> {
            throw getCompletionException(exceptionSupplier.get());
        };
    }

    public static <T> Function<Throwable, T> throwIn(Looper looper) {
        return throwIn(HandlerExecutor.forLooper(looper));
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
    /** Has to be followed by {@link #throwIn} in the end of the chain. */
    @SafeVarargs
    public static <T, E extends Throwable> Function<Throwable, T> catching(
            Consumer<E> consumer, Class<? extends E>... classes) {
        return catching(
                (Function<E, T>) e -> {
                    consumer.accept(e);
                    // Will ignore this exception in throwIn()
                    throw new ExceptionCaught(e);
                },
                classes);
    }

    @SafeVarargs
    public static <T, E extends Throwable> Function<Throwable, T> catching(
            Function<E, T> function, Class<? extends E>... classes) {
        return throwable -> {
            Throwable e =
                    (throwable instanceof CompletionException) ? throwable.getCause() : throwable;
            Optional<Class<? extends E>> caught =
                    Stream.of(classes).filter(c -> c.isInstance(e)).findFirst();
            if (caught.isPresent()) {
                return function.apply(caught.get().cast(e));
            }
            throw getCompletionException(e);
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
