package com.brufino.android.playground.transfer.task.tasks.multi.subtasks;

import android.content.Intent;
import android.os.DeadObjectException;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.RemoteException;
import com.brufino.android.common.IProducer;
import com.brufino.android.common.TransferUtils;
import com.brufino.android.playground.extensions.ThrowingRunnable;
import com.brufino.android.playground.extensions.service.ServiceClient;
import com.brufino.android.playground.extensions.service.ServiceClientFactory;
import com.brufino.android.playground.transfer.task.TaskController;
import com.brufino.android.playground.transfer.task.TransferTask;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static com.brufino.android.playground.transfer.task.tasks.TaskUtils.readFromProducer;
import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;

public class ProducerReaderSubTask implements ThrowingRunnable<Exception> {
    private final ServiceClientFactory mClientFactory;
    private final Intent mProducerIntent;
    private final TaskController mController;
    private final ParcelFileDescriptor mSubTaskOutput;

    ProducerReaderSubTask(
            ServiceClientFactory serviceClientFactory,
            TaskController controller,
            ParcelFileDescriptor subTaskOutput) {
        mClientFactory = serviceClientFactory;
        mController = controller;
        mSubTaskOutput = subTaskOutput;
        mProducerIntent =
                TransferUtils.getProducerIntent(TransferTask.PRODUCER_PACKAGE);
    }

    public void run() throws RemoteException, IOException, InterruptedException, TimeoutException {
        ServiceClient<IProducer> producerClient =
                mClientFactory.getServiceClient(mProducerIntent, IProducer.Stub::asInterface);
        try {
            IProducer producer = producerClient.connect();
            mController.configure(producer);
            ParcelFileDescriptor[] producerPipe = ParcelFileDescriptor.createPipe();

            read(producer, producerPipe);
        } finally {
            producerClient.disconnect();
        }
    }

    public void read(IProducer producer, ParcelFileDescriptor[] producerPipe)
            throws RemoteException, IOException, TimeoutException {
        producer.produce(0, producerPipe[1]);
        producerPipe[1].close();

        try (DataInputStream input =
                     new DataInputStream(new AutoCloseInputStream(producerPipe[0]));
             OutputStream output = new AutoCloseOutputStream(mSubTaskOutput)) {
            byte[] buffer = new byte[mController.getBufferSize()];
            transfer(input, output, buffer);
        }
    }

    private void transfer(DataInputStream input, OutputStream output, byte[] buffer)
            throws IOException, TimeoutException {
        // TODO(brufino): Add time-out
        long deadline = currentTimeMillis() + TransferTask.TASK_TIME_OUT_MS;
        int chunkSize;
        while ((chunkSize = input.readInt()) > 0) {
            if (currentTimeMillis() > deadline) {
                throw new TimeoutException("Producer read timed out");
            }
            while (chunkSize > 0) {
                int sizeRead =
                        readFromProducer(mController, input, buffer, min(chunkSize, buffer.length));
                if (sizeRead < 0) {
                    throw new EOFException("Unexpected EOF");
                }
                output.write(buffer, 0, sizeRead);
                chunkSize -= sizeRead;
            }
        }
    }
}
