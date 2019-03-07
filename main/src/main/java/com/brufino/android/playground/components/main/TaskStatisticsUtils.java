package com.brufino.android.playground.components.main;

import com.brufino.android.playground.transfer.task.TaskEntry;
import com.brufino.android.playground.transfer.TransferConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.brufino.android.playground.extensions.collection.StreamUtils.toOrderedMap;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.averagingLong;
import static java.util.stream.Collectors.groupingBy;

public class TaskStatisticsUtils {
    /** Returns a map ordered by values (time). */
    public static Map<Parameters, Double> computeTimesByParameters(List<TaskEntry> history) {
        return history
                .stream()
                .collect(
                        groupingBy(
                                TaskStatisticsUtils::getParameters,
                                averagingLong(taskEntry -> taskEntry.duration)))
                .entrySet()
                .stream()
                .sorted(comparingDouble(Map.Entry::getValue))
                .collect(toOrderedMap());
    }

    private static Parameters getParameters(TaskEntry taskEntry) {
        TransferConfiguration configuration = taskEntry.configuration;
        return new Parameters(
                taskEntry.name,
                configuration.producerDataSize,
                configuration.producerInterval,
                configuration.producerChunkSize,
                configuration.transferBufferSize,
                configuration.consumerInterval,
                configuration.consumerBufferSize);
    }

    private TaskStatisticsUtils() {}

    public static class Parameters {
        public final String taskName;
        public final int producerDataSize;
        public final int producerInterval;
        public final int producerChunkSize;
        public final int transferBufferSize;
        public final int consumerInterval;
        public final int consumerBufferSize;

        private Parameters(
                String taskName,
                int producerDataSize,
                int producerInterval,
                int producerChunkSize,
                int transferBufferSize,
                int consumerInterval,
                int consumerBufferSize) {
            this.taskName = taskName;
            this.producerDataSize = producerDataSize;
            this.producerInterval = producerInterval;
            this.producerChunkSize = producerChunkSize;
            this.transferBufferSize = transferBufferSize;
            this.consumerInterval = consumerInterval;
            this.consumerBufferSize = consumerBufferSize;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Parameters)) {
                return false;
            }
            Parameters that = (Parameters) other;
            return producerDataSize == that.producerDataSize
                    && producerInterval == that.producerInterval
                    && producerChunkSize == that.producerChunkSize
                    && transferBufferSize == that.transferBufferSize
                    && consumerInterval == that.consumerInterval
                    && consumerBufferSize == that.consumerBufferSize
                    && Objects.equals(taskName, that.taskName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    taskName,
                    producerDataSize,
                    producerInterval,
                    producerChunkSize,
                    transferBufferSize,
                    consumerInterval,
                    consumerBufferSize);
        }
    }
}
