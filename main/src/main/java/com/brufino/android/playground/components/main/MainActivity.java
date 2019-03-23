package com.brufino.android.playground.components.main;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.PagerAdapter;
import com.brufino.android.playground.MainConstants;
import com.brufino.android.playground.components.main.pages.statistics.StatisticsFragment;
import com.brufino.android.playground.extensions.permission.PermissionRequester;
import com.brufino.android.playground.provision.Provisioners;
import com.brufino.android.playground.R;
import com.brufino.android.playground.components.main.pages.aggregate.AggregateFragment;
import com.brufino.android.playground.components.main.pages.history.HistoryFragment;
import com.brufino.android.playground.databinding.ActivityMainBinding;
import com.brufino.android.playground.transfer.TransferCancellationFailedException;
import com.brufino.android.playground.transfer.task.TaskEntry;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.extensions.*;
import com.brufino.android.playground.components.main.TaskStatisticsUtils.Parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static com.brufino.android.common.CommonConstants.TAG;
import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.playground.components.main.TaskStatisticsUtils.computeTimesByParameters;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.execute;
import static com.brufino.android.playground.extensions.livedata.LiveDataUtils.futureLiveData;
import static java.util.function.Function.identity;

// TODO(brufino): Use CopyOnWriteArrayList instead of simple array list when we are defending
public class MainActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private final MainActivityProvisioner mProvisioner;
    private TransferManager mTransferManager;
    private ExecutorService mWorkExecutor;
    private MainViewModel mData;
    private PermissionRequester mPermissionRequester;
    private TaskSheet mTaskSheet;

    public MainActivity() {
        mProvisioner = Provisioners.get().getMainActivityProvisioner(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTransferManager = mProvisioner.getTransferManager();
        mPermissionRequester = mProvisioner.getPermissionRequester();
        mWorkExecutor = mProvisioner.getWorkExecutor();
        mTaskSheet = mProvisioner.getTaskSheet();
        ViewModelProvider.Factory viewModelFactory = mProvisioner.getViewModelFactory();

        mData = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel.class);
        mData.onActivityCreate(this);
        mData.sheetFuture.observe(this, this::onSheetFutureChanged);
        mData.cancelFuture.observe(this, this::onCancelFutureChanged);

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);
        binding.setViewModel(mData);
        binding.setActions(new Actions());
        PagerAdapter adapter =
                FixedFragmentsPagerAdapter.builder(getSupportFragmentManager())
                        .add("Log", HistoryFragment::new)
                        .add("Rank", AggregateFragment::new)
                        .add("Stats", StatisticsFragment::new)
                        .build();
        binding.display.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionRequester.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onCancelFutureChanged(@Nullable CompletableFuture<Void> future) {
        if (future == null || !future.isDone()) {
            return;
        }
        try {
            future.get();
            Toast.makeText(this, "Task cancelled", Toast.LENGTH_SHORT).show();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TransferCancellationFailedException) {
                Log.w(TAG, "Cancel failed and task completed", cause);
            } else {
                Log.w(TAG, "Cancel failed because task prematurely abort", cause);
            }
            Toast.makeText(this, "Cancellation failed", Toast.LENGTH_SHORT).show();
        } catch (InterruptedException e) {
            throw new AssertionError("Impossible since future is guaranteed to be done");
        }
    }

    private void onSheetFutureChanged(@Nullable CompletableFuture<Intent> future) {
        if (future == null || !future.isDone()) {
            return;
        }
        try {
            Intent intent = future.get();
            startActivity(intent);
        } catch (ExecutionException e) {
            Log.e(TAG, "Export sheet failed", e);
            Toast.makeText(this, "Export sheet failed", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Export sheet failed", e);
            Toast.makeText(this, "No apps to open csv", Toast.LENGTH_SHORT).show();
        } catch (InterruptedException e) {
            throw new AssertionError("Impossible since future is guaranteed to be done");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        Log.d(TAG, "MainActivity.onSaveInstanceState()");
    }

    public class Actions {
        public void startTask(@TransferManager.Code int code) {
            PermissionRequester permissionRequester = mPermissionRequester;
            TransferManager transferManager = mTransferManager;
            execute(
                    () -> {
                        permissionRequester.requestPermission(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        transferManager.enqueueTransfer(code, TransferConfiguration.DEFAULT);
                    },
                    mWorkExecutor);
        }

        public void cancelTask() {
            CompletableFuture<Void> future =
                    execute(mTransferManager::cancel, mWorkExecutor)
                            .thenComposeAsync(identity(), mWorkExecutor);
            futureLiveData(mData.cancelFuture, future);
        }

        public void clearQueue() {
            execute(mTransferManager::clearQueue, mWorkExecutor);
        }

        public void clearHistory() {
            execute(mTransferManager::clearHistory, mWorkExecutor);
        }

        public void exportSheet() {
            PermissionRequester permissionRequester = mPermissionRequester;
            List<TaskEntry> history = mTransferManager.getLiveHistory().getValue();
            Context context = getApplicationContext();
            CompletableFuture<Intent> future = execute(
                    () -> {
                        if (!permissionRequester.requestPermission(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            throw new UnsupportedOperationException("User denied permission");
                        }
                        Thread.sleep(500);
                        Map<Parameters, Double> results =
                                computeTimesByParameters(checkNotNull(history));
                        Path sheet = mTaskSheet.save(results);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri =
                                FileProvider.getUriForFile(
                                        context, MainConstants.PROVIDER_AUTHORITY, sheet.toFile());
                        intent.setDataAndType(uri, "text/csv");
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        return intent;
                    },
                    mWorkExecutor);
            futureLiveData(mData.sheetFuture, future);
        }
    }
}
