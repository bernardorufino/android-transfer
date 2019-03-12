package com.brufino.android.playground.provision;

import android.app.Activity;
import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.brufino.android.playground.components.main.TaskSheet;
import com.brufino.android.playground.components.main.pages.history.HistoryAdapter;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.extensions.concurrent.AppThreadFactory;
import com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils;
import com.brufino.android.playground.extensions.livedata.LiveDataPersisterFactory;
import com.brufino.android.playground.extensions.permission.PermissionRequester;
import com.brufino.android.playground.extensions.service.ServiceClientFactory;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.transfer.task.TaskHistory;
import com.brufino.android.playground.transfer.task.TaskManager;
import com.brufino.android.playground.transfer.task.tasks.TaskFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static com.brufino.android.common.utils.Preconditions.checkArgument;
import static com.brufino.android.common.utils.Preconditions.checkState;

class Provisioner {
    private TransferManager mTransferManager;

    private final Object mTaskManagerLock = new Object();

    @GuardedBy("mTaskManagerLock")
    private volatile TaskManager mTaskManager;

    private final Object mWorkExecutorLock = new Object();

    @GuardedBy("mWorkExecutorLock")
    private volatile ExecutorService mWorkExecutor;

    private final Object mRequestExecutorLock = new Object();

    @GuardedBy("mRequestExecutorLock")
    private volatile ExecutorService mRequestExecutor;

    private final Object mIoExecutorLock = new Object();

    @GuardedBy("mIoExecutorLock")
    private volatile ExecutorService mIoExecutor;

    @MainThread
    TransferManager getTransferManager(ApplicationContext context) {
        checkState(ConcurrencyUtils.isMainThread(), "Can't create TransferManager on background thread");
        if (mTransferManager == null) {
            mTransferManager =
                    new TransferManager(context, getTaskManager(context), getWorkExecutor());
        }
        return mTransferManager;
    }

    PermissionRequester getPermissionRequester(Activity activity) {
        return new PermissionRequester(activity);
    }

    ExecutorService getWorkExecutor() {
        if (mWorkExecutor == null) {
            synchronized (mWorkExecutorLock) {
                if (mWorkExecutor == null) {
                    mWorkExecutor =
                            Executors.newCachedThreadPool(new AppThreadFactory("work-executor-%d"));
                }
            }
        }
        return mWorkExecutor;
    }

    ExecutorService getRequestExecutor() {
        if (mRequestExecutor == null) {
            synchronized (mRequestExecutorLock) {
                if (mRequestExecutor == null) {
                    mRequestExecutor =
                            Executors.newFixedThreadPool(
                                    6, new AppThreadFactory("request-executor-%d"));
                }
            }
        }
        return mRequestExecutor;
    }

    private ExecutorService getIoExecutor() {
        if (mIoExecutor == null) {
            synchronized (mIoExecutorLock) {
                if (mIoExecutor == null) {
                    // Assuming 80% IO and 4 cores
                    mIoExecutor =
                            Executors.newFixedThreadPool(
                                    20, new AppThreadFactory("io-executor-%d"));
                }
            }
        }
        return mIoExecutor;
    }

    TaskManager getTaskManager(ApplicationContext context) {
        if (mTaskManager == null) {
            synchronized (mTaskManagerLock) {
                if (mTaskManager == null) {
                    mTaskManager =
                            new TaskManager(
                                    context,
                                    getTaskFactory(context),
                                    getTaskHistory(context));
                }
            }
        }
        return mTaskManager;
    }

    TaskSheet getTaskSheet(ApplicationContext context) {
        return new TaskSheet(context);
    }

    <T extends ViewModel> ViewModelProvider.Factory getViewModelFactory(
            Class<T> viewModelClass,
            Supplier<? extends T> supplier) {
        return new ViewModelProvider.Factory() {
            @SuppressWarnings("unchecked")
            @Override
            public <U extends ViewModel> U create(Class<U> childClass) {
                checkArgument(viewModelClass.isAssignableFrom(childClass));
                return (U) supplier.get();
            }
        };
    }

    LiveDataPersisterFactory getPersisterFactory(ApplicationContext context) {
        return new LiveDataPersisterFactory(context);
    }

    HistoryAdapter getHistoryAdapter(LifecycleOwner owner) {
        return new HistoryAdapter(owner);
    }

    private TaskHistory getTaskHistory(ApplicationContext context) {
        return new TaskHistory(context, getIoExecutor(), getWorkExecutor());
    }

    private TaskFactory getTaskFactory(ApplicationContext context) {
        return new TaskFactory(context, getServiceClientFactory(context));
    }

    private ServiceClientFactory getServiceClientFactory(ApplicationContext context) {
        return new ServiceClientFactory(context, getWorkExecutor());
    }
}
