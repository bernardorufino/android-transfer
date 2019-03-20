package com.brufino.android.playground.transfer.task.tasks.single;

import android.content.Intent;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.RemoteException;
import android.util.Log;
import com.brufino.android.common.IConsumer;
import com.brufino.android.common.IProducer;
import com.brufino.android.common.TransferUtils;
import com.brufino.android.playground.extensions.service.ServiceClientFactory;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.transfer.task.TaskController;
import com.brufino.android.playground.transfer.task.TransferTask;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.extensions.service.ServiceClient;

import java.io.*;
import java.util.concurrent.TimeoutException;

import static com.brufino.android.common.CommonConstants.TAG;
import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.common.utils.Preconditions.checkState;
import static com.brufino.android.playground.transfer.task.tasks.TaskUtils.readFromProducer;
import static com.brufino.android.playground.transfer.task.tasks.TaskUtils.sendDataReceivedToConsumer;
import static com.brufino.android.playground.transfer.task.tasks.TaskUtils.writeToConsumer;
import static java.lang.Math.min;

public class SingleThreadTask extends TransferTask {
    private final Intent mProducerIntent;
    private final Intent mConsumerIntent;
    private final ServiceClientFactory mClientFactory;
    private final TaskController mController;
    private Thread mThread;

    public SingleThreadTask(
            ApplicationContext context,
            ServiceClientFactory serviceClientFactory,
            Looper looper,
            TransferConfiguration configuration) {
        super(context, looper, configuration, "Single");
        mClientFactory = serviceClientFactory;
        mProducerIntent =
                TransferUtils.getProducerIntent(TransferTask.PRODUCER_PACKAGE);
        mConsumerIntent =
                TransferUtils.getConsumerIntent(TransferTask.CONSUMER_PACKAGE);
        mController = getController();
    }

    @Override
    protected void onStart() {
        mThread = new Thread(this::run, "single-task");
        mThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            checkNotNull(Looper.myLooper()).quitSafely();
        }
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
            mController.configure(producer);
            mController.configure(consumer);
            ParcelFileDescriptor[] producerPipe = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor[] consumerPipe = ParcelFileDescriptor.createPipe();

            transfer(producer, consumer, producerPipe, consumerPipe);
        } catch (RemoteException | IOException | TimeoutException e) {
            // TODO(brufino): Don't crash the process
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Log.i(TAG, getName() + " task thread interrupted");
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
            throws RemoteException, IOException, TimeoutException {
        producer.produce(0, producerPipe[1]);
        producerPipe[1].close();

        consumer.start(consumerPipe[0]);
        consumerPipe[0].close();

        try (DataInputStream input =
                     new DataInputStream(new AutoCloseInputStream(producerPipe[0]));
             OutputStream output = new AutoCloseOutputStream(consumerPipe[1])) {
            byte[] buffer = new byte[mController.getBufferSize()];
            transfer(consumer, input, output, buffer);
        }

        consumer.finish();
    }

    private void transfer(
            IConsumer consumer,
            DataInputStream input,
            OutputStream output,
            byte[] buffer) throws IOException, RemoteException, TimeoutException {
        boolean tracing = mController.startTracing();
        long deadline = System.currentTimeMillis() + TASK_TIME_OUT_MS;
        int chunkSize;
        while ((chunkSize = input.readInt()) > 0) {
            if (System.currentTimeMillis() > deadline) {
                throw new TimeoutException("Transfer timed out");
            }
            while (chunkSize > 0) {
                int sizeRead =
                        readFromProducer(mController, input, buffer, min(chunkSize, buffer.length));
                if (sizeRead < 0) {
                    throw new EOFException("Unexpected EOF");
                }
                writeToConsumer(mController, output, buffer, sizeRead);
                sendDataReceivedToConsumer(mController, consumer, sizeRead);
                chunkSize -= sizeRead;
            }
        }
        mController.stopTracing(tracing);
    }

}
