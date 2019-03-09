package com.brufino.android.playground.transfer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;
import com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils;
import com.brufino.android.playground.provision.Provisioners;

import java.util.concurrent.ExecutorService;

import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.common.utils.Preconditions.checkState;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.execute;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.isMainThread;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.resettingInterrupt;

public abstract class TransferManagerService extends LifecycleService {
    private final TransferManagerServiceProvisioner mProvisioner;
    private TransferManager mTransferManager;
    private ExecutorService mRequestExecutor;

    public TransferManagerService() {
        mProvisioner = Provisioners.get().getTransferManagerServiceProvisioner(this);
    }

    protected abstract void onHandleRequest(TransferRequest request);

    @MainThread
    protected abstract void onStart();

    /** Call {@code super.onCreate()} after your method body. */
    @Override
    @CallSuper
    public void onCreate() {
        super.onCreate();
        mTransferManager = mProvisioner.getTransferManager();
        mRequestExecutor = mProvisioner.getRequestExecutor();
        resettingInterrupt(() -> mTransferManager.onServiceCreated(this));
    }

    /**
     * After this method returns any request made via {@link TransferManager#enqueueTransfer(int,
     * TransferConfiguration)} will be fulfilled via Android start-service infra-structure, *not*
     * directly via {@link TransferManagerService#request(TransferRequest, int)}. Also there
     * won't be any direct calls to the service.
     */
    @Override
    @CallSuper
    public void onDestroy() {
        super.onDestroy();
        // We can't just swallow the interrupt because we don't know how to stop the main-thread
        // (which is the thread that executes this), even though the service is being stopped.
        resettingInterrupt(mTransferManager::onServiceDestroy);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }

    @MainThread
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        checkState(isMainThread());

        if (intent == null) {
            return Service.START_NOT_STICKY;
        }
        onStart();
        TransferRequest request = TransferRequest.create(intent);
        Log.d("BMReq", "request(START_SERVICE/" + startId + ")");
        onHandleRequest(request);
        return Service.START_REDELIVER_INTENT;
    }

    /** MUST only be called from {@link TransferManager}. */
    void request(TransferRequest request, int startId) {
        Log.d("BMReq", "request(TRANSFER_MANAGER/" + startId + ")");
        onHandleRequest(request);
    }

    protected void retry(TransferRequest request) {
        TransferManager transferManager = mTransferManager;
        execute(
                mRequestExecutor,
                () -> transferManager.enqueueTransfer(request.code, request.configuration));
    }
}
