package com.brufino.android.playground.transfer.task.tasks.multi.subtasks;

import android.content.Intent;
import android.os.DeadObjectException;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.RemoteException;
import com.brufino.android.common.IConsumer;
import com.brufino.android.common.TransferUtils;
import com.brufino.android.playground.extensions.ThrowingRunnable;
import com.brufino.android.playground.extensions.service.ServiceClient;
import com.brufino.android.playground.extensions.service.ServiceClientFactory;
import com.brufino.android.playground.transfer.task.TaskController;
import com.brufino.android.playground.transfer.task.TransferTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static com.brufino.android.common.utils.Preconditions.checkArgument;
import static com.brufino.android.common.utils.Preconditions.checkState;
import static com.brufino.android.playground.transfer.task.tasks.TaskUtils.sendDataReceivedToConsumer;
import static com.brufino.android.playground.transfer.task.tasks.TaskUtils.writeToConsumer;
import static java.lang.System.currentTimeMillis;

public class ConsumerWriterSubTask implements ThrowingRunnable<Exception> {
    private final ServiceClientFactory mClientFactory;
    private final Intent mConsumerIntent;
    private final TaskController mController;
    private final ParcelFileDescriptor mSubTaskInput;

    ConsumerWriterSubTask(
            ServiceClientFactory serviceClientFactory,
            TaskController controller,
            ParcelFileDescriptor subTaskInput) {
        mClientFactory = serviceClientFactory;
        mController = controller;
        mSubTaskInput = subTaskInput;
        mConsumerIntent =
                TransferUtils.getConsumerIntent(TransferTask.CONSUMER_PACKAGE);
    }

    public void run() throws RemoteException, IOException, InterruptedException, TimeoutException {
        ServiceClient<IConsumer> consumerClient =
                mClientFactory.getServiceClient(mConsumerIntent, IConsumer.Stub::asInterface);
        try {
            IConsumer consumer = consumerClient.connect();
            mController.configure(consumer);
            ParcelFileDescriptor[] consumerPipe = ParcelFileDescriptor.createPipe();

            write(consumer, consumerPipe);
        } finally {
            consumerClient.disconnect();
        }
    }

    private void write(IConsumer consumer, ParcelFileDescriptor[] consumerPipe)
            throws IOException, RemoteException, TimeoutException {
        consumer.start(consumerPipe[0]);
        consumerPipe[0].close();

        try (InputStream input = new AutoCloseInputStream(mSubTaskInput);
             OutputStream output = new AutoCloseOutputStream(consumerPipe[1])) {
            byte[] buffer = new byte[mController.getBufferSize()];
            write(consumer, input, output, buffer);
        }

        consumer.finish();
    }

    private void write(
            IConsumer consumer,
            InputStream input,
            OutputStream output,
            byte[] buffer) throws IOException, RemoteException, TimeoutException {
        checkArgument(buffer.length > 0);
        long deadline = currentTimeMillis() + TransferTask.TASK_TIME_OUT_MS;
        int sizeRead;
        while ((sizeRead = input.read(buffer)) > 0) {
            if (currentTimeMillis() > deadline) {
                throw new TimeoutException("Consumer write timed out");
            }
            writeToConsumer(mController, output, buffer, sizeRead);
            sendDataReceivedToConsumer(mController, consumer, sizeRead);
        }
        // End of file
        checkState(sizeRead == -1);
    }
}
