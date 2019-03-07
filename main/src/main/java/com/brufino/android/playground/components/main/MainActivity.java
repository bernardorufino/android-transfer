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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.PagerAdapter;
import com.brufino.android.common.CommonConstants;
import com.brufino.android.playground.MainConstants;
import com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils;
import com.brufino.android.playground.extensions.permission.PermissionRequester;
import com.brufino.android.playground.provision.Provisioners;
import com.brufino.android.playground.R;
import com.brufino.android.playground.components.main.pages.aggregate.AggregateFragment;
import com.brufino.android.playground.components.main.pages.history.HistoryFragment;
import com.brufino.android.playground.databinding.ActivityMainBinding;
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

import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.execute;

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
        mData.setSheetFutureObserver(this, this::onSheetFutureChanged);

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);
        binding.setViewModel(mData);
        binding.setActions(new Actions());
        PagerAdapter adapter =
                FixedFragmentsPagerAdapter.builder(getSupportFragmentManager())
                        .add("History", HistoryFragment::new)
                        .add("Aggregate", AggregateFragment::new)
                        .build();
        binding.display.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionRequester.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onSheetFutureChanged(CompletableFuture<Intent> future) {
        if (future == null || !future.isDone()) {
            return;
        }
        try {
            Intent intent = future.get();
            startActivity(intent);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(CommonConstants.TAG, "Export sheet failed", e);
            Toast.makeText(this, "Export sheet failed", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Log.e(CommonConstants.TAG, "Export sheet failed", e);
            Toast.makeText(this, "No apps to open csv", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        Log.d(CommonConstants.TAG, "MainActivity.onSaveInstanceState()");
    }

    public class Actions {
        public void startTask(@TransferManager.Code int code) {
            PermissionRequester permissionRequester = mPermissionRequester;
            TransferManager transferManager = mTransferManager;
            execute(
                    mWorkExecutor,
                    () -> {
                        permissionRequester.requestPermission(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        transferManager.enqueueTransfer(code, TransferConfiguration.DEFAULT);
                    });
        }

        public void debug() {
            execute(
                            mWorkExecutor,
                            () -> {
                                Thread.sleep(1000);
                                return "foo";
                            })
                    .thenAcceptAsync(
                            value ->
                                    Toast
                                            .makeText(
                                                    MainActivity.this, value, Toast.LENGTH_SHORT)
                                            .show(),
                            ConcurrencyUtils.getMainThreadExecutor());

        }

        public void clearQueue() {
            TransferManager transferManager = mTransferManager;
            execute(mWorkExecutor, transferManager::clearQueue);
        }

        public void clearHistory() {
            mTransferManager.clearHistory();
        }

        public void exportSheet() {
            PermissionRequester permissionRequester = mPermissionRequester;
            List<TaskEntry> history = mTransferManager.getLiveHistory().getValue();
            Context context = getApplicationContext();
            CompletableFuture<Intent> future = execute(
                    mWorkExecutor,
                    () -> {
                        if (!permissionRequester.requestPermission(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            throw new UnsupportedOperationException("User denied permission");
                        }
                        Thread.sleep(500);
                        Map<Parameters, Double> results =
                                TaskStatisticsUtils.computeTimesByParameters(history);
                        Path sheet = mTaskSheet.save(results);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri =
                                FileProvider.getUriForFile(
                                        context, MainConstants.PROVIDER_AUTHORITY, sheet.toFile());
                        intent.setDataAndType(uri, "text/csv");
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        return intent;
                    });
            mData.setSheetFuture(future);
        }
    }
}
