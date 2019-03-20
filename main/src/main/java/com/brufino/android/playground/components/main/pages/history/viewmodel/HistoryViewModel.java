package com.brufino.android.playground.components.main.pages.history.viewmodel;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.brufino.android.playground.R;
import com.brufino.android.playground.extensions.collection.StreamUtils;
import com.brufino.android.playground.extensions.livedata.transform.Transform;
import com.brufino.android.playground.transfer.task.TaskEntry;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.extensions.ViewUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static com.brufino.android.common.CommonConstants.TAG_TASK;
import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.playground.extensions.ViewUtils.getStackTrace;
import static com.brufino.android.playground.extensions.ViewUtils.sizeString;
import static java.util.stream.Collectors.toList;

public class HistoryViewModel extends AndroidViewModel {
    public final LiveData<List<HistoryItemViewModel>> history;

    public HistoryViewModel(
            Application application,
            TransferManager transferManager,
            ExecutorService workExecutor) {
        super(application);
        history =
                Transform.source(transferManager.getLiveHistory())
                        .map(CopyOnWriteArrayList::new)
                        .mutate(Collections::reverse, workExecutor)
                        .map(this::getViewModelList, workExecutor)
                        .getLiveData();
    }

    /** Header + Entries */
    private List<HistoryItemViewModel> getViewModelList(List<TaskEntry> history) {
        return Stream
                .concat(
                        Stream.of(getHistoryHeaderViewModel(history)),
                        StreamUtils
                                .withIndex(history)
                                .map(this::getHistoryEntryViewModel))
                .collect(toList());
    }

    private HistoryItemViewModel getHistoryHeaderViewModel(List<TaskEntry> history) {
        long totalTasks = history.size();
        long successfulTasks = history.stream().filter(TaskEntry::succeeded).count();
        return new HistoryHeaderViewModel(
                totalTasks, successfulTasks, totalTasks - successfulTasks);
    }

    private HistoryItemViewModel getHistoryEntryViewModel(Pair<TaskEntry, Long> indexedTask) {
        TaskEntry task = indexedTask.first;
        long index = indexedTask.second;
        int backgroundColorRes =
                (index % 2 == 0) ? R.color.darkBackground3 : R.color.darkBackground2;
        int alternateColorRes =
                (index % 2 == 0) ? R.color.darkBackground2 : R.color.darkBackground3;
        return new HistoryEntryViewModel(
                getApplication().getColor(backgroundColorRes),
                task.name,
                // Won't throw when / 0 bc it's double
                String.format(
                        Locale.US,
                        "%.2f KB/s",
                        (double) task.outputWritten / task.duration / 1000),
                String.format(
                        Locale.US,
                        "%s/%.2fs",
                        sizeString(task.outputWritten, 5),
                        (double) task.duration / 1000),
                getApplication()
                        .getColor(
                                task.inputRead == task.outputWritten
                                        ? backgroundColorRes
                                        : alternateColorRes),
                String.format(Locale.US, "%dms", task.configuration.producerInterval),
                sizeString(task.configuration.producerChunkSize, 5),
                sizeString(task.configuration.transferBufferSize, 5),
                String.format(Locale.US, "%dms", task.configuration.consumerInterval),
                sizeString(task.configuration.consumerBufferSize, 5),
                !task.succeeded(),
                view -> logTaskDetails(view.getContext(), task),
                view -> logTaskError(view.getContext(), task));
    }

    private void logTaskError(Context context, TaskEntry task) {
        Toast
                .makeText(context, "Error logged as TransferTask", Toast.LENGTH_SHORT)
                .show();
        Log.d(TAG_TASK, getStackTrace(checkNotNull(task.exception)));
    }

    private void logTaskDetails(Context context, TaskEntry task) {
        Toast
                .makeText(context, "Task logged as TransferTask", Toast.LENGTH_SHORT)
                .show();
        Log.d(TAG_TASK, task.toMultilineString(1));
    }

}

