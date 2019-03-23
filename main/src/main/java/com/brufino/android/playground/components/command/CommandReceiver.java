package com.brufino.android.playground.components.command;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.brufino.android.playground.provision.Provisioners;
import com.brufino.android.playground.transfer.TransferConfiguration;
import com.brufino.android.playground.transfer.TransferManager;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static com.brufino.android.common.CommonConstants.TAG;
import static com.brufino.android.common.utils.Preconditions.checkArgument;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.execute;

// TODO: If app not open we lose the broadcasts, handle this either here or in bash
// TODO: Stop/cancel
public class CommandReceiver extends BroadcastReceiver {
    private static final String ACTION_START_TRANSFER = "com.brufino.android.START_TRANSFER";
    private static final String ACTION_CLEAR_QUEUE = "com.brufino.android.CLEAR_QUEUE";
    private static final String ACTION_CLEAR_HISTORY = "com.brufino.android.CLEAR_HISTORY";
    private static final String EXTRA_TASK = "task";
    private static final String EXTRA_PRODUCER_DATA = "producer_data";
    private static final String EXTRA_PRODUCER_INTERVAL = "producer_interval";
    private static final String EXTRA_PRODUCER_CHUNK = "producer_chunk";
    private static final String EXTRA_TRANSFER_BUFFER = "transfer_buffer";
    private static final String EXTRA_CONSUMER_BUFFER = "consumer_buffer";
    private static final String EXTRA_CONSUMER_INTERVAL = "consumer_interval";
    private static final String EXTRA_REPEAT = "repeat";

    private final CommandReceiverProvisioner mProvisioner;

    public CommandReceiver() {
        mProvisioner = Provisioners.get().getCommandReceiverProvisioner();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        TransferManager transferManager = mProvisioner.getTransferManager(context);
        ExecutorService workExecutor = mProvisioner.getWorkExecutor();
        ExecutorService requestExecutor = mProvisioner.getRequestExecutor();
        execute(() -> onReceiveWork(transferManager, requestExecutor, intent), workExecutor);
    }

    private void onReceiveWork(
            TransferManager transferManager, Executor requestExecutor, Intent intent)
            throws InterruptedException {
        switch (intent.getAction()) {
            case ACTION_START_TRANSFER:
                startTransfer(transferManager, requestExecutor, intent);
                break;
            case ACTION_CLEAR_QUEUE:
                clearQueue(transferManager, intent);
                break;
            case ACTION_CLEAR_HISTORY:
                clearHistory(transferManager, intent);
                break;
            default:
                throw new IllegalArgumentException("Can't understand intent " + intent.getAction());
        }
        Log.d(TAG, intent + " processed");
    }

    private void startTransfer(
            TransferManager manager, Executor requestExecutor, Intent intent) {
        int repeat = getNonNegativeIntExtra(intent, EXTRA_REPEAT, 1);
        int code = toTaskCode(intent.getStringExtra(EXTRA_TASK));
        TransferConfiguration configuration =
                new TransferConfiguration(
                        getRequiredNonNegativeIntExtra(intent, EXTRA_PRODUCER_DATA),
                        getRequiredNonNegativeIntExtra(intent, EXTRA_PRODUCER_INTERVAL),
                        getRequiredNonNegativeIntExtra(intent, EXTRA_PRODUCER_CHUNK),
                        getRequiredNonNegativeIntExtra(intent, EXTRA_TRANSFER_BUFFER),
                        getRequiredNonNegativeIntExtra(intent, EXTRA_CONSUMER_INTERVAL),
                        getRequiredNonNegativeIntExtra(intent, EXTRA_CONSUMER_BUFFER));
         for (int i = 0; i < repeat; i++) {
             execute(() -> manager.enqueueTransfer(code, configuration), requestExecutor);
         }
    }

    private void clearQueue(TransferManager manager, Intent intent) throws InterruptedException {
        manager.clearQueue();
    }

    private void clearHistory(TransferManager manager, Intent intent) {
        manager.clearHistory();
    }

    private int getRequiredNonNegativeIntExtra(Intent intent, String extra) {
        return getNonNegativeIntExtra(intent, extra, -1);
    }

    private int getNonNegativeIntExtra(Intent intent, String extra, int defaultValue) {
        int value = intent.getIntExtra(extra, defaultValue);
        checkArgument(value >= 0, "Invalid " + extra + " = " + value);
        return value;
    }

    @TransferManager.Code
    private int toTaskCode(String task) {
        task = task.toLowerCase();
        if (task.contains("single")) {
            return TransferManager.Code.SINGLE_THREAD;
        }
        if (task.contains("multi")) {
            return TransferManager.Code.MULTI_THREAD;
        }
        throw new IllegalArgumentException("Unknown task " + task);
    }
}
