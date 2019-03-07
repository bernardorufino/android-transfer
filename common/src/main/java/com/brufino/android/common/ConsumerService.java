package com.brufino.android.common;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import androidx.annotation.Nullable;

public abstract class ConsumerService extends Service {
    public static final String ACTION = "com.brufino.android.CONSUMER";

    private final IConsumer mBinder = new ConsumerBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder.asBinder();
    }

    public abstract void configure(int bufferSize, long intervalMs);

    public abstract void start(ParcelFileDescriptor input);

    public abstract void onDataReceived(int bytes);

    public abstract void finish();

    public class ConsumerBinder extends IConsumer.Stub {
        @Override
        public void configure(int bufferSize, long intervalMs) throws RemoteException {
            ConsumerService.this.configure(bufferSize, intervalMs);
        }
        @Override
        public void start(ParcelFileDescriptor input) throws RemoteException {
            ConsumerService.this.start(input);
        }
        @Override
        public void onDataReceived(int bytes) throws RemoteException {
            ConsumerService.this.onDataReceived(bytes);
        }
        @Override
        public void finish() throws RemoteException {
            ConsumerService.this.finish();
        }
    }
}
