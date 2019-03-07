package com.brufino.android.playground.extensions.permission;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.ArrayMap;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;
import androidx.core.app.ActivityCompat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.brufino.android.common.utils.Preconditions.checkState;

// TODO(brufino): Support multiple permission requests
public class PermissionRequester {
    private final Activity mActivity;
    private final Object mRequestsLock = new Object();
    private final Map<Integer, CompletableFuture<Boolean>> mCurrentRequests =
            new ArrayMap<>();
    private int mRequests = 0;

    public PermissionRequester(Activity activity) {
        mActivity = activity;
    }

    @WorkerThread
    public boolean requestPermission(String permission) throws InterruptedException {
        checkState(
                !Looper.getMainLooper().isCurrentThread(),
                "Cannot request permission on main thread");

        if (PermissionUtils.hasPermission(mActivity, permission)) {
            return true;
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final int requestCode;
        synchronized (mRequestsLock) {
            requestCode = mRequests;
            mRequests++;
            mCurrentRequests.put(requestCode, future);
        }
        ActivityCompat.requestPermissions(mActivity, new String[] {permission}, requestCode);
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new AssertionError("Future can't terminate exceptionally", e);
        }
    }

    /**
     * Has to be called from
     * {@link ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}
     */
    @MainThread
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        final CompletableFuture<Boolean> future;
        synchronized (mRequestsLock) {
             future = mCurrentRequests.remove(requestCode);
        }
        future.complete(grantResults[0] == PackageManager.PERMISSION_GRANTED);
    }
}
