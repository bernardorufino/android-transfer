package com.brufino.android.playground.transfer.task.tasks.single;

import android.content.Intent;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.RemoteException;
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
    // TODO(brufino): Inject taskExecutor and use it instead of spinning a new thread here 
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
        mThread = new Thread(this::runTask, "single-task");
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

    private void runTask() {
        try {
            run();
        } catch (RemoteException | InterruptedException | IOException | TimeoutException e) {
            abortTask(e);
            return;
        }
        finishTask();
    }

    /** Will connect, run, then disconnect clients. */
    private void run()
            throws RemoteException, InterruptedException, IOException, TimeoutException {
        ServiceClient<IProducer> producerClient =
                mClientFactory.getServiceClient(mProducerIntent, IProducer.Stub::asInterface);
        ServiceClient<IConsumer> consumerClient =
                mClientFactory.getServiceClient(mConsumerIntent, IConsumer.Stub::asInterface);
        producerClient.connectAsync();
        consumerClient.connectAsync();
        try {
            run(producerClient.get(), consumerClient.get());
        } finally {
            consumerClient.disconnect();
            producerClient.disconnect();
        }
    }

    /** We have producer and consumer connected. */
    private void run(IProducer producer, IConsumer consumer)
            throws RemoteException, IOException, TimeoutException {
        mController.configure(producer);
        mController.configure(consumer);
        ParcelFileDescriptor[] producerPipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor[] consumerPipe = ParcelFileDescriptor.createPipe();
        transfer(producer, consumer, producerPipe, consumerPipe);
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
