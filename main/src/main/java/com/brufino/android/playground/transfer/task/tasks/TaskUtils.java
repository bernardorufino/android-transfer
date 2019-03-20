package com.brufino.android.playground.transfer.task.tasks;

import android.os.RemoteException;
import com.brufino.android.common.IConsumer;
import com.brufino.android.playground.MainConstants;
import com.brufino.android.playground.transfer.task.TaskController;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.brufino.android.common.utils.Preconditions.checkState;

public class TaskUtils {
    public static int readFromProducer(
            TaskController controller,
            DataInputStream input,
            byte[] buffer,
            int sizeToRead) throws IOException {
        TaskController.Stopwatch time = controller.startTime("read");
        int sizeRead = input.read(buffer, 0, sizeToRead);
        time.stop();
        controller.addInputRead(sizeRead);
        return sizeRead;
    }

    public static void writeToConsumer(
            TaskController controller,
            OutputStream output,
            byte[] buffer,
            int sizeToWrite) throws IOException {
        // This is because the pipe would be stuck waiting for consumer to consumer the data and
        // we won't have a chance to call onDataReceived() to signal the consumer that the data
        // has been sent.
        checkState(sizeToWrite <= MainConstants.PIPE_SIZE, "Can't write to pipe > 64 KB");
        TaskController.Stopwatch time = controller.startTime("write");
        output.write(buffer, 0, sizeToWrite);
        time.stop();
        controller.addOutputWritten(sizeToWrite);
    }

    public static void sendDataReceivedToConsumer(
            TaskController controller,
            IConsumer consumer,
            int sizeRead) throws RemoteException {
        TaskController.Stopwatch time = controller.startTime("onDataReceived");
        consumer.onDataReceived(sizeRead);
        time.stop();
    }

    private TaskUtils() {}
}
