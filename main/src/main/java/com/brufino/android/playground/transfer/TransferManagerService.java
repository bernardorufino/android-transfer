package com.brufino.android.playground.transfer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;
import com.brufino.android.playground.provision.Provisioners;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.WeakHashMap;

import static com.brufino.android.common.utils.Preconditions.checkNotNull;
import static com.brufino.android.common.utils.Preconditions.checkState;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.isMainThread;
import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.resettingInterrupt;
import static java.util.Collections.synchronizedMap;

public abstract class TransferManagerService extends LifecycleService {
    private static final int[] JOB_ORIGINS = {
            RequestOrigin.START_SERVICE, RequestOrigin.TRANSFER_MANAGER
    };

    private final Map<TransferRequest, TransferRequestMetadata> mRequests =
            synchronizedMap(new WeakHashMap<>());
    private final TransferManagerServiceProvisioner mProvisioner;
    private TransferManager mTransferManager;

    /**
     * Apart from onCreate() mLastRequests is only accessed in tryStop(), which always runs in
     * one specific thread.
     */
    private TransferRequestMetadata[] mLastRequests;

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
        mLastRequests = new TransferRequestMetadata[JOB_ORIGINS.length];
        resettingInterrupt(() -> mTransferManager.onServiceCreated(this));
    }

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
        Log.d("BMReq", "onStartCommand()");
        checkState(isMainThread());

        if (intent == null) {
            return Service.START_NOT_STICKY;
        }
        onStart();
        TransferRequest request = TransferRequest.create(intent);
        Log.d("BMReq", "onStartCommand() -> request()");
        request(request, RequestOrigin.START_SERVICE, startId);
        return Service.START_REDELIVER_INTENT;
    }

    /** MUST only be called from {@link TransferManager}. */
    void request(TransferRequest request, int startId) {
        request(request, RequestOrigin.TRANSFER_MANAGER, startId);
    }

    private void request(TransferRequest request, @RequestOrigin int origin, int startId) {
        // Log.d("BMReq", "request(request = " + request + ", origin = " + originToString(origin) +
        //         ", startId = " + startId + ")");
        TransferRequestMetadata metadata = new TransferRequestMetadata(origin, startId, request);
        Log.d("BMReq", "request(" + metadata + ")");
        mRequests.put(request, metadata);
        onHandleRequest(request);
    }

    private static String originToString(int origin) {
        switch (origin) {
            case RequestOrigin.START_SERVICE:
                return "START_SERVICE";
            case RequestOrigin.TRANSFER_MANAGER:
                return "TRANSFER_MANAGER";
            default:
                throw new AssertionError();
        }
    }

    /**
     * Tries to stop the service after processing of {@code request}. This method should always
     * be called from one specific thread.
     *
     * This dance is a bit tricky. First we check if we can stop the service via {@link
     * TransferManager}. If yes, then we try to stop the service via Android service
     * infra-structure. If we succeed then stop the service with {@link TransferManager}.
     */
    protected boolean tryStop(TransferRequest request) throws InterruptedException {
        TransferRequestMetadata metadata = checkNotNull(mRequests.get(request));
        Log.d("BMReq", "tryStop(" + metadata + ")");
        mLastRequests[metadata.origin] = metadata;
        TransferRequestMetadata lastStartRequest = mLastRequests[RequestOrigin.START_SERVICE];
        TransferRequestMetadata lastManagerRequest = mLastRequests[RequestOrigin.TRANSFER_MANAGER];
        if (lastStartRequest == null) {
            // Can't stop without the first request via startService() has been processed, otherwise
            // we can't pass anything to stopSelfResult(). It also implies we still have a
            // request in the queue, the first startService() request that created the service,
            // so we don't want to stop anyway.
            return false;
        }
        int startServiceId = lastStartRequest.id;
        int transferManagerId =
                lastManagerRequest != null
                        ? lastManagerRequest.id
                        : TransferManager.IDLE_LAST_START_ID;
        Log.d("BMReq", "  => mLastRequests = {START_SERVICE/" + startServiceId + ", " +
                "TRANSFER_MANAGER/" + transferManagerId + "}");
        if (!mTransferManager.tryBeginStopService(transferManagerId)) {
            return false;
        }
        boolean stopped = false;
        try {
            stopped = stopSelfResult(startServiceId);
        } finally {
            mTransferManager.finishStopService(stopped);
        }
        return stopped;
    }

    @IntDef({RequestOrigin.START_SERVICE, RequestOrigin.TRANSFER_MANAGER})
    @Retention(RetentionPolicy.SOURCE)
    @interface RequestOrigin {
        int START_SERVICE = 0;
        int TRANSFER_MANAGER = 1;
    }

    private static class TransferRequestMetadata {
        final int id;
        final @RequestOrigin int origin;
        final TransferRequest request;

        private TransferRequestMetadata(
                @RequestOrigin int origin, int id, TransferRequest request) {
            this.id = id;
            this.origin = origin;
            this.request = request;
        }

        @Override
        public String toString() {
            return "TransferRequestMetadata{"
                    + originToString(origin) + "/" + id +'}';
        }
    }
}
