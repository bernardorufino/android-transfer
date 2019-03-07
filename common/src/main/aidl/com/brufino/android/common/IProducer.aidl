package com.brufino.android.common;

import android.os.ParcelFileDescriptor;

interface IProducer {
    void configure(int dataSize, int chunkSize, long intervalMs);
    oneway void produce(int code, in ParcelFileDescriptor output);
}
