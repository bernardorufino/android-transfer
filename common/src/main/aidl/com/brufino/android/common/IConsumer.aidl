package com.brufino.android.common;

import android.os.ParcelFileDescriptor;

interface IConsumer {
    void configure(int bufferSize, long intervalMs);
    void start(in ParcelFileDescriptor input);
    void onDataReceived(int bytes);
    void finish();
}
