package com.brufino.android.playground.transfer.task.tasks;

import android.content.Intent;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.RemoteException;
import android.util.Log;
import com.brufino.android.common.CommonConstants;
import com.brufino.android.common.IConsumer;
import com.brufino.android.common.IProducer;
import com.brufino.android.common.PlaygroundUtils;
import com.brufino.android.playground.extensions.service.ServiceClientFactory;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.transfer.task.TransferTask;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.extensions.service.ServiceClient;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.brufino.android.common.utils.Preconditions.checkState;

public class SingleThreadTask extends TransferTask {
    private final Intent mProducerIntent;
    private final Intent mConsumerIntent;
    private final ServiceClientFactory mClientFactory;

    SingleThreadTask(
            ApplicationContext context,
            ServiceClientFactory serviceClientFactory,
            Looper looper,
            TransferConfiguration configuration) {
        super(context, looper, configuration, "Single");
        mClientFactory = serviceClientFactory;
        mProducerIntent =
                PlaygroundUtils.getProducerIntent(TransferTask.PRODUCER_PACKAGE);
        mConsumerIntent =
                PlaygroundUtils.getConsumerIntent(TransferTask.CONSUMER_PACKAGE);
    }

    @Override
    public void onStart() {
        new Thread(this::run, "single-thread-transfer").start();
    }

    private void run() {
        ServiceClient<IProducer> producerClient =
                mClientFactory.getServiceClient(mProducerIntent, IProducer.Stub::asInterface);
        ServiceClient<IConsumer> consumerClient =
                mClientFactory.getServiceClient(mConsumerIntent, IConsumer.Stub::asInterface);

        try {
            producerClient.connectAsync();
            consumerClient.connectAsync();
            IProducer producer = producerClient.get();
            IConsumer consumer = consumerClient.get();

            ParcelFileDescriptor[] producerPipe = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor[] consumerPipe = ParcelFileDescriptor.createPipe();

            configure(producer, consumer);
            transfer(producer, consumer, producerPipe, consumerPipe);
        } catch (RemoteException | IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Log.i(CommonConstants.TAG, getName() + " task thread interrupted");
        }

        consumerClient.disconnect();
        producerClient.disconnect();
        finishTask();
    }

    private void transfer(
            IProducer producer,
            IConsumer consumer,
            ParcelFileDescriptor[] producerPipe,
            ParcelFileDescriptor[] consumerPipe)
            throws RemoteException, IOException {
        producer.produce(0, producerPipe[1]);
        producerPipe[1].close();

        consumer.start(consumerPipe[0]);
        consumerPipe[0].close();

        try (DataInputStream input =
                     new DataInputStream(new AutoCloseInputStream(producerPipe[0]));
             OutputStream output = new AutoCloseOutputStream(consumerPipe[1])) {
            byte[] buffer = new byte[getBufferSize()];
            transfer(consumer, input, output, buffer);
        }
    }

    private void transfer(
            IConsumer consumer,
            DataInputStream input,
            OutputStream output,
            byte[] buffer) throws IOException, RemoteException {
        boolean tracing = startTracing();
        long deadline = System.currentTimeMillis() + PRODUCER_TIME_OUT_MS;
        int chunkSize;
        while ((chunkSize = input.readInt()) > 0 && System.currentTimeMillis() < deadline) {
            while (chunkSize > 0) {
                int sizeToRead = (chunkSize > buffer.length) ? buffer.length : chunkSize;
                checkState(sizeToRead > 0);
                int sizeRead = read(input, buffer, sizeToRead);
                if (sizeRead < 0) {
                    throw new EOFException("Unexpected EOF");
                }
                write(output, buffer, sizeRead);
                consumerOnDataReceived(consumer, sizeRead);
                chunkSize -= sizeRead;
            }
        }
        consumer.finish();
        stopTracing(tracing);
    }

    private void consumerOnDataReceived(IConsumer consumer, int sizeRead) throws RemoteException {
        // Stopwatch time = startTime("onDataReceived");
        consumer.onDataReceived(sizeRead);
        // time.stop();
    }

    private int read(DataInputStream input, byte[] buffer, int sizeToRead) throws IOException {
        // Stopwatch time = startTime("read");
        int sizeRead = input.read(buffer, 0, sizeToRead);
        inputRead(sizeRead);
        // time.stop();
        return sizeRead;
    }

    private void write(OutputStream output, byte[] buffer, int sizeToWrite) throws IOException {
        // Stopwatch time = startTime("write");
        output.write(buffer, 0, sizeToWrite);
        outputWritten(sizeToWrite);
        // time.stop();
    }
}
