package com.brufino.android.common;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public abstract class ProducerService extends Service {
    public static final String ACTION = "com.brufino.android.PRODUCER";

    private final IProducer mBinder = new ProducerBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder.asBinder();
    }

    public abstract void configure(int dataSize, int chunkSize, long intervalMs);

    public abstract void produce(int code, ParcelFileDescriptor output);

    private class ProducerBinder extends IProducer.Stub {
        @Override
        public void configure(int dataSize, int chunkSize, long intervalMs) throws RemoteException {
            ProducerService.this.configure(dataSize, chunkSize, intervalMs);
        }
        @Override
        public void produce(int code, ParcelFileDescriptor output) throws RemoteException {
            ProducerService.this.produce(code, output);
        }


    }
}
