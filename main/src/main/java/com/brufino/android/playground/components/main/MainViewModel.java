package com.brufino.android.playground.components.main;

import android.app.Application;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.lifecycle.*;
import com.brufino.android.playground.extensions.ViewUtils;
import com.brufino.android.playground.extensions.livedata.LiveDataPersister;
import com.brufino.android.playground.extensions.livedata.LiveDataPersisterFactory;
import com.brufino.android.playground.extensions.livedata.LiveDataUtils;
import com.brufino.android.playground.extensions.livedata.transform.Transform;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.transfer.TransferManager.Code;
import com.brufino.android.playground.transfer.TransferRequest;
import com.brufino.android.playground.transfer.task.TaskInformation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.util.stream.Collectors.joining;

public class MainViewModel extends AndroidViewModel {
    private static final String PREFERENCE_CURRENT_TAB = "current_tab";
    private static final int THROUGHPUT_AVERAGE_ELEMENTS = 20;

    /** Used for {@link TaskInformation#getTimeElapsed()}} be up-to-date. */
    private static final int UPDATE_INTERVAL_MS = 200;

    public final LiveData<String> queueStatus;
    public final LiveData<String> transferStatus;
    public final LiveData<String> serviceStatus;
    public final MutableLiveData<Integer> currentTab;
    public final LiveData<Boolean> loadingSheet;
    public final LiveData<String> sheetButton;
    private final Observer<CompletableFuture<Intent>> mSheetFutureObserver;
    private final MutableLiveData<CompletableFuture<Intent>> mSheetFuture;
    private final LiveDataPersister mPersister;

    public MainViewModel(
            Application application,
            TransferManager transferManager,
            LiveDataPersisterFactory persisterFactory,
            ExecutorService workExecutor) {
        super(application);
        mPersister =
                persisterFactory.getPersister(MainActivity.class, getClass().getCanonicalName());
        transferStatus =
                Transform.source(transferManager.getLiveTaskInformation())
                        .map(this::getTransferStatus, workExecutor)
                        .getLiveData();
        serviceStatus =
                Transform.source(transferManager.getLiveTaskThroughput())
                        .accumulateLast(THROUGHPUT_AVERAGE_ELEMENTS)
                        .map(this::getMovingAverage, workExecutor)
                        .combine(
                                transferManager.getLiveTaskThroughput(),
                                this::getServiceStatus,
                                workExecutor)
                        .getLiveData();
        queueStatus =
                Transform.source(transferManager.getLiveQueue())
                        .map(this::getQueueStatus, workExecutor)
                        .getLiveData();
        currentTab = new MutableLiveData<>();
        mSheetFuture = LiveDataUtils.mutableLiveData(null);
        mSheetFutureObserver = this::onSheetFutureChanged;
        loadingSheet =
                Transform.source(mSheetFuture)
                        .map(future -> future != null && !future.isDone(), workExecutor)
                        .getLiveData();
        sheetButton =
                Transform.source(loadingSheet)
                        .map(loading -> (loading) ? "Loading..." : "Export Sheet", workExecutor)
                        .getLiveData();
    }

    void onActivityCreate(LifecycleOwner owner) {
        mSheetFuture.observe(owner, mSheetFutureObserver);
        mPersister.persist(owner, currentTab, PREFERENCE_CURRENT_TAB, 0);
    }

    private void onSheetFutureChanged(@Nullable CompletableFuture<Intent> future) {
        if (future != null && future.isDone()) {
            // Reset LiveData
            mSheetFuture.postValue(null);
        }
    }

    void setSheetFutureObserver(
            LifecycleOwner owner, Observer<? super CompletableFuture<Intent>> observer) {
        mSheetFuture.removeObserver(mSheetFutureObserver);
        mSheetFuture.observe(
                owner,
                future -> {
                    observer.onChanged(future);
                    mSheetFutureObserver.onChanged(future);
                });
    }

    void setSheetFuture(CompletableFuture<Intent> future) {
        LiveDataUtils.futureAsync(mSheetFuture, future);
    }

    private double getMovingAverage(List<Double> elements) {
        return elements.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private String getQueueStatus(List<TransferRequest> queue) {
        String prefix = String.format(Locale.US, "Queue (%s): ", queue.size());
        return prefix
                + queue
                        .stream()
                        .map(request -> codeToString(request.code))
                        .collect(joining(", "));

    }

    private static String codeToString(@Code int code) {
        switch (code) {
            case Code.SINGLE_THREAD:
                return "S";
            case Code.MULTI_THREAD:
                return "M";
            default:
                throw new IllegalArgumentException("Unknown code " + code);
        }
    }

    private String getServiceStatus(double averageThroughput, double throughput) {
        return
                String.format(
                        Locale.US,
                        "Throughput: %.0f tasks/minute\n"
                            + "Last %d avg: %.0f tasks/minute",
                        throughput,
                        THROUGHPUT_AVERAGE_ELEMENTS,
                        averageThroughput);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private String getTransferStatus(
            Optional<TaskInformation> taskInformationOptional) {
        if (!taskInformationOptional.isPresent()) {
            return "IDLE";
        }
        TaskInformation taskInformation = taskInformationOptional.get();
        return "Running " + taskInformation.name + " ("
                + ViewUtils.sizeString(taskInformation.outputWritten, 5) + " / "
                + taskInformation.getTimeElapsed() + " ms)\n";
    }
}
