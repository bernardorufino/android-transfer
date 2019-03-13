package com.brufino.android.consumer;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.brufino.android.common.CommonConstants;
import com.brufino.android.common.ConsumerService;
import com.brufino.android.common.utils.Preconditions;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import static com.brufino.android.common.CommonConstants.TAG;
import static com.brufino.android.common.utils.Preconditions.checkState;

public class AppConsumer extends ConsumerService {
    private int mBufferSize = -1;
    private int mBytesReceived = 0;
    private long mIntervalMs = -1;
    private DataInputStream mInputStream;

    @Override
    public void configure(int bufferSize, long intervalMs) {
        mBufferSize = bufferSize;
        mIntervalMs = intervalMs;
    }

    @Override
    public void start(ParcelFileDescriptor input) {
        checkState(mBufferSize > 0);
        mInputStream = new DataInputStream(new ParcelFileDescriptor.AutoCloseInputStream(input));
    }

    @Override
    public void onDataReceived(int bytes) {
        checkState(mBufferSize > 0);
        checkState(mIntervalMs >= 0);

        try {
            Log.d(TAG, "Received " + bytes + " (" + mBytesReceived + " total)");
            byte[] buffer = new byte[mBufferSize];
            while (bytes > 0) {
                Thread.sleep(mIntervalMs);
                int sizeToRead = (bytes > buffer.length) ? buffer.length : bytes;
                int sizeRead = mInputStream.read(buffer, 0, sizeToRead);
                if (sizeRead < 0) {
                    throw new EOFException("Unexpected EOF");
                }
                mBytesReceived += sizeRead;
                Log.d(TAG, "=> Read " + mBytesReceived + " (+" + sizeRead + ")");
                bytes -= sizeRead;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finish() {
        try {
            mInputStream.close();
            Log.d(TAG, "Closing stream");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
