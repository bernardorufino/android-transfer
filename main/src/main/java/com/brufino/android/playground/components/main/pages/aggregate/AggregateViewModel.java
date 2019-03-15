package com.brufino.android.playground.components.main.pages.aggregate;

import android.app.Application;
import android.view.View;
import android.widget.TableRow;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.brufino.android.playground.components.main.MainActivity;
import com.brufino.android.playground.components.main.TaskStatisticsUtils;
import com.brufino.android.playground.components.main.TaskStatisticsUtils.Parameters;
import com.brufino.android.playground.databinding.TableCell;
import com.brufino.android.playground.extensions.livedata.LiveDataPersister;
import com.brufino.android.playground.extensions.livedata.LiveDataPersisterFactory;
import com.brufino.android.playground.extensions.livedata.transform.Transform;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.transfer.task.TaskEntry;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static java.util.function.Function.identity;

public class AggregateViewModel extends AndroidViewModel {
    private static final String PREFERENCE_ROW_MASK = "row_mask";
    private static final String[] HEADER = {
            "\n", "Pd\nKB", "Pi\nms", "Pc\nKB", "Tb\nKB", "Ci\nms", "Cb\nKB", "T\nms"
    };
    private static final int COLUMNS = HEADER.length;

    public final LiveData<String[][]> table;
    private final LiveDataPersister mPersister;
    private final MutableLiveData<Integer> mColumnMask;

    public AggregateViewModel(
            Application application,
            TransferManager transferManager,
            LiveDataPersisterFactory persisterFactory,
            ExecutorService workExecutor) {
        super(application);
        mPersister =
                persisterFactory.getPersister(MainActivity.class, getClass().getCanonicalName());
        mColumnMask = new MutableLiveData<>();
        table =
                Transform.source(transferManager.getLiveHistory())
                        .combine(mColumnMask, this::getTable, workExecutor)
                        .getLiveData();
    }

    void onActivityCreate(LifecycleOwner owner) {
        mPersister.persist(owner, mColumnMask, PREFERENCE_ROW_MASK, getDefaultColumnMask());
    }

    private String[][] getTable(List<TaskEntry> history, int mask) {
        return Stream.of(
                        Stream.<String[]>of(HEADER),
                        TaskStatisticsUtils.computeTimesByParameters(history)
                                .entrySet()
                                .stream()
                                .map(entry -> {
                                    Parameters parameters = entry.getKey();
                                    return new String[] {
                                            parameters.taskName.substring(0, 1),
                                            sizeString(parameters.producerDataSize),
                                            String.format(Locale.US, "%d",
                                                    parameters.producerInterval),
                                            sizeString(parameters.producerChunkSize),
                                            sizeString(parameters.transferBufferSize),
                                            String.format(Locale.US, "%d",
                                                    parameters.consumerInterval),
                                            sizeString(parameters.consumerBufferSize),
                                            String.format(Locale.US, "%.0f", entry.getValue())
                                    };
                                }))
                .flatMap(identity())
                .map(row -> applyColumnMask(mask, row))
                .toArray(String[][]::new);
    }

    public void onCellClick(View cellView) {
        TableCell<String> cell = TableCell.from(cellView);
        TableRow row = (TableRow) cellView.getParent();
        // Do useful stuff with row
    }

    public boolean onCellLongClick(View cellView) {
        TableCell<String> cell = TableCell.from(cellView);
        if (cell.i == 0 && cell.j == 0) {
            // Reset
            mColumnMask.setValue(getDefaultColumnMask());
            return true;
        }
        if (cell.i != 0) {
            // Only collapse header
            return false;
        }
        int mask = checkNotNull(mColumnMask.getValue());
        int i = getFullIndex(mask, cell.j);
        mColumnMask.setValue(mask ^ (1 << i));
        return true;
    }

    private static int getFullIndex(int mask, int partialIndex) {
        for (int i = 0, j = 0; i < COLUMNS; i++) {
            if (isColumnVisible(mask, i)) {
                if (j == partialIndex) {
                    return i;
                }
                j++;
            }
        }
        throw new IllegalArgumentException("Can't find partial index " + partialIndex);
    }

    private static boolean isColumnVisible(int mask, int column) {
        return ((mask >> column) & 1) != 0;
    }

    private static String[] applyColumnMask(int mask, String[] row) {
        return IntStream.range(0, row.length)
                .filter(i -> isColumnVisible(mask, i))
                .mapToObj(i -> row[i])
                .toArray(String[]::new);
    }

    private static int getDefaultColumnMask() {
        return ~0;
    }

    private static String sizeString(int sizeBytes) {
        return String.format(Locale.US, "%.0f", (double) sizeBytes / 1024);
    }
}
