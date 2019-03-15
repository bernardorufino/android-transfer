package com.brufino.android.playground.extensions.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.DeadObjectException;
import android.os.IBinder;
import com.brufino.android.playground.extensions.ApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.asyncThrowing;

// TODO(brufino): I need a new CONNECTING state because of onServiceDisconnected() and
//                consecutive connects(). Do I??
/**
 * Obtain an object of this class and call {@link #connect()} followed by a {@link #disconnect()}
 * only once and both from the same thread.
 */
public class ServiceClient<T> {
    private final Context mContext;
    private final Intent mServiceIntent;
    private final CompletableFuture<IBinder> mBinderFuture;
    private final CompletableFuture<T> mInterfaceFuture;
    private final Connection mConnection;

    ServiceClient(
            ApplicationContext context,
            ExecutorService workExecutor,
            Intent serviceIntent,
            Function<IBinder, T> converter) {
        mContext = context.getContext();
        mServiceIntent = serviceIntent;
        mConnection = new Connection();
        mBinderFuture = new CompletableFuture<>();
        mInterfaceFuture =
                mBinderFuture
                        .thenApplyAsync(
                                asyncThrowing(ServiceClient::checkBinderNotNull), workExecutor)
                        .thenApplyAsync(converter, workExecutor);
    }

    public T connect() throws DeadObjectException, InterruptedException {
        connectAsync();
        return get();
    }

    /** Call {@link #get} afterwards. */
    public CompletableFuture<T> connectAsync() {
        if (mBinderFuture.isDone()) {
            throw new IllegalStateException("connect() or connectAsync() already called");
        }
        mContext.bindService(mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
        return mInterfaceFuture;

    }

    public T get() throws DeadObjectException, InterruptedException {
        try {
            return mInterfaceFuture.get();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof DeadObjectException) {
                throw (DeadObjectException) cause;
            }
            throw new RuntimeException(exception);
        }
    }

    private static IBinder checkBinderNotNull(IBinder binder) throws DeadObjectException {
        if (binder == null) {
            throw new DeadObjectException("Binder null");
        }
        return binder;
    }

    /** Has to be called from the same thread as {@link #connect()} or {@link #connectAsync()}. */
    public void disconnect() {
        if (!mBinderFuture.isDone()) {
            throw new IllegalStateException("disconnect() called before connect()");
        }
        mContext.unbindService(mConnection);
    }

    private class Connection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinderFuture.complete(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Binder object will throw DOE
        }
    }
}
