package com.brufino.android.playground.transfer.task.tasks.multi.subtasks;

import android.os.ParcelFileDescriptor;
import com.brufino.android.playground.extensions.service.ServiceClientFactory;
import com.brufino.android.playground.transfer.task.TaskController;

public class MultiSubTaskFactory {
    private final ServiceClientFactory mServiceClientFactory;

    public MultiSubTaskFactory(ServiceClientFactory serviceClientFactory) {
        mServiceClientFactory = serviceClientFactory;
    }

    public ProducerReaderSubTask getReaderSubTask(TaskController controller, ParcelFileDescriptor output) {
        return new ProducerReaderSubTask(mServiceClientFactory, controller, output);
    }

    public ConsumerWriterSubTask getWriterSubTask(TaskController controller, ParcelFileDescriptor input) {
        return new ConsumerWriterSubTask(mServiceClientFactory, controller, input);
    }
}
