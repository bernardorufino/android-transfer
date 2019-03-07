package com.brufino.android.playground.extensions.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.util.Log;
import com.brufino.android.playground.extensions.ApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

// TODO(brufino): I need a new CONNECTING state because of onServiceDisconnected() and
//                consecutive connects(). Do I??
/**
 * Obtain an object of this class and call {@link #connect()} followed by a {@link #disconnect()}
 * only once and both from the same thread.
 */
public class ServiceClient<T> {
    private final Function<IBinder, T> mConverter;
    private final Context mContext;
    private final Intent mServiceIntent;
    private final CompletableFuture<IBinder> mBinderFuture = new CompletableFuture<>();
    private final Connection mConnection = new Connection();

    ServiceClient(
            ApplicationContext context, Intent serviceIntent, Function<IBinder, T> converter) {
        mContext = context.getContext();
        mServiceIntent = serviceIntent;
        mConverter = converter;
    }

    public T connect() throws DeadObjectException {
        if (mBinderFuture.isDone()) {
            throw new IllegalStateException("connect() called twice");
        }
        mContext.bindService(mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
        final IBinder binder;
        try {
            binder = mBinderFuture.get();
            // long startTime = System.nanoTime();
            // long midTime = System.nanoTime();
            // long finalTime = System.nanoTime();
            // Log.d("BML", "1st = " + TimeUnit.NANOSECONDS.toMillis(midTime - startTime) + " ms");
            // Log.d("BML", "2nd = " + TimeUnit.NANOSECONDS.toMillis(finalTime - midTime) + " ms");
        } catch (ExecutionException | InterruptedException e) {
            throw new DeadObjectException("Cause: " + e.getMessage());
        }
        if (binder == null) {
            throw new DeadObjectException("Null binder");
        }
        return mConverter.apply(binder);
    }

    /** Has to be called from the same thread as {@link #connect()}. */
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
            // TODO(brufino)
        }
    }
}
