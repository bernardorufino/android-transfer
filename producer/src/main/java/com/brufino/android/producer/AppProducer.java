package com.brufino.android.producer;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.brufino.android.common.CommonConstants;
import com.brufino.android.common.ProducerService;
import com.brufino.android.common.utils.Preconditions;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.brufino.android.common.CommonConstants.TAG;
import static com.brufino.android.common.utils.Preconditions.checkArgument;
import static com.brufino.android.common.utils.Preconditions.checkState;

public class AppProducer extends ProducerService {
    private int mDataSize = -1;
    private int mChunkSize = -1;
    private long mIntervalMs = -1;
    private int mBytesSent = 0;

    @Override
    public void configure(int dataSize, int chunkSize, long intervalMs) {
        mDataSize = dataSize;
        mChunkSize = chunkSize;
        mIntervalMs = intervalMs;
    }

    @Override
    public void produce(int code, ParcelFileDescriptor outputFd) {
        checkState(mDataSize > 0);
        checkState(mChunkSize > 0);
        checkState(mIntervalMs >= 0);

        try {
            DataOutputStream output =
                    new DataOutputStream(new FileOutputStream(outputFd.getFileDescriptor()));

            byte[] buffer = new byte[mChunkSize];
            int remainingBytes = mDataSize;
            while (remainingBytes > 0) {
                Thread.sleep(mIntervalMs);
                writeBytes(output, buffer);
                remainingBytes -= buffer.length;
            }

            writeEndOfData(output);
            output.close();
            outputFd.close();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeBytes(DataOutputStream output, byte[] data) throws IOException {
        output.writeInt(data.length);
        output.write(data);
        output.flush();
        mBytesSent += data.length;
        Log.d(TAG, "Sent " + mBytesSent + " bytes");
    }

    private void writeString(DataOutputStream output, String string) throws IOException {
        checkArgument(!string.isEmpty());
        writeBytes(output, string.getBytes());
    }

    private void writeEndOfData(DataOutputStream output) throws IOException {
        output.writeInt(0);
        output.flush();
    }
}
