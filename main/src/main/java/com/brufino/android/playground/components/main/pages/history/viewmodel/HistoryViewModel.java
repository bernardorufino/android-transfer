package com.brufino.android.playground.components.main.pages.history.viewmodel;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.brufino.android.common.CommonConstants;
import com.brufino.android.playground.R;
import com.brufino.android.playground.extensions.collection.StreamUtils;
import com.brufino.android.playground.extensions.livedata.transform.Transform;
import com.brufino.android.playground.transfer.task.TaskEntry;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.extensions.ViewUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import static com.brufino.android.common.CommonConstants.TAG;
import static java.util.stream.Collectors.toList;

public class HistoryViewModel extends AndroidViewModel {
    public final LiveData<List<HistoryEntryViewModel>> history;

    public HistoryViewModel(
            Application application,
            TransferManager transferManager,
            ExecutorService workExecutor) {
        super(application);
        history =
                Transform.source(transferManager.getLiveHistory())
                        .mutate(Collections::reverse, workExecutor)
                        .map(
                                history ->
                                        StreamUtils
                                                .withIndex(history)
                                                .map(this::getHistoryEntryViewModel)
                                                .collect(toList()),
                                workExecutor)
                        .getLiveData();
    }

    private HistoryEntryViewModel getHistoryEntryViewModel(Pair<TaskEntry, Long> indexedTask) {
        TaskEntry task = indexedTask.first;
        long index = indexedTask.second;
        boolean sizesDiffer = task.inputRead != task.outputWritten;
        if (sizesDiffer) {
            Log.e(
                    TAG,
                    String.format(
                            "Task %s input read = %s but output written = %s",
                            task.name,
                            task.inputRead,
                            task.outputWritten));
        }
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
                        ViewUtils.sizeString(task.outputWritten, 5),
                        (double) task.duration / 1000),
                getApplication().getColor((sizesDiffer) ? alternateColorRes : backgroundColorRes),
                String.format(Locale.US, "%dms", task.configuration.producerInterval),
                ViewUtils.sizeString(task.configuration.producerChunkSize, 5),
                ViewUtils.sizeString(task.configuration.transferBufferSize, 5),
                String.format(Locale.US, "%dms", task.configuration.consumerInterval),
                ViewUtils.sizeString(task.configuration.consumerBufferSize, 5),
                view -> logTaskDetails(view.getContext(), task));
    }

    private void logTaskDetails(Context context, TaskEntry task) {
        Toast
                .makeText(context, "Task logged as TransferTask", Toast.LENGTH_SHORT)
                .show();
        Log.d(CommonConstants.TAG_TASK, task.toMultilineString(1));
    }

}

