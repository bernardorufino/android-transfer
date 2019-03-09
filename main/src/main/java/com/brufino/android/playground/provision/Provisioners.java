package com.brufino.android.playground.provision;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.brufino.android.playground.components.command.CommandReceiverProvisioner;
import com.brufino.android.playground.components.main.MainActivityProvisioner;
import com.brufino.android.playground.components.main.MainViewModel;
import com.brufino.android.playground.components.main.TaskSheet;
import com.brufino.android.playground.components.main.pages.aggregate.AggregateFragmentProvisioner;
import com.brufino.android.playground.components.main.pages.aggregate.AggregateViewModel;
import com.brufino.android.playground.components.main.pages.history.HistoryAdapter;
import com.brufino.android.playground.components.main.pages.history.HistoryFragmentProvisioner;
import com.brufino.android.playground.components.main.pages.history.viewmodel.HistoryViewModel;
import com.brufino.android.playground.transfer.TransferManagerServiceProvisioner;
import com.brufino.android.playground.transfer.task.TaskManager;
import com.brufino.android.playground.transfer.TransferManager;
import com.brufino.android.playground.transfer.service.TransferServiceProvisioner;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.extensions.permission.PermissionRequester;

import java.util.concurrent.ExecutorService;

/**
 * Important: DO NOT use the Context / Activity / Fragment provided in the methods here, just
 * pass them along. These methods are usually called in the referred component's constructor,
 * which means the OS hasn't created (remember onCreate()) the objects properly yet, so
 * interactions with these objects could result in unexpected behavior or crashes.
 */
public class Provisioners {
    private static final Object INSTANCE_LOCK = new Object();

    @SuppressLint("StaticFieldLeak")
    @GuardedBy("INSTANCE_LOCK")
    private static volatile Provisioners sInstance;
    public static Provisioners get() {
        if (sInstance == null) {
            synchronized (INSTANCE_LOCK) {
                if (sInstance == null) {
                    sInstance = new Provisioners();
                }
            }
        }
        return sInstance;
    }

    private final Provisioner mProvisioner;

    private Provisioners() {
        mProvisioner = new Provisioner();
    }

    public MainActivityProvisioner getMainActivityProvisioner(Activity activity) {
        return new MainActivityProvisioner() {
            @Override
            public PermissionRequester getPermissionRequester() {
                return mProvisioner.getPermissionRequester(activity);
            }

            @MainThread
            @Override
            public TransferManager getTransferManager() {
                return mProvisioner.getTransferManager(ApplicationContext.from(activity));
            }

            @Override
            public ExecutorService getWorkExecutor() {
                return mProvisioner.getWorkExecutor();
            }

            @Override
            public ViewModelProvider.Factory getViewModelFactory() {
                return mProvisioner.getViewModelFactory(
                        MainViewModel.class,
                        () ->
                                new MainViewModel(
                                        activity.getApplication(),
                                        getTransferManager(),
                                        mProvisioner.getPersisterFactory(
                                                        ApplicationContext.from(activity)),
                                        mProvisioner.getWorkExecutor()));
            }

            @Override
            public TaskSheet getTaskSheet() {
                return mProvisioner.getTaskSheet(ApplicationContext.from(activity));
            }
        };
    }

    public AggregateFragmentProvisioner getAggregateFragmentProvisioner(Fragment fragment) {
        return new AggregateFragmentProvisioner() {
            @MainThread
            @Override
            public TransferManager getTransferManager() {
                return mProvisioner.getTransferManager(ApplicationContext.from(fragment));
            }

            @Override
            public ViewModelProvider.Factory getViewModelFactory() {
                return mProvisioner.getViewModelFactory(
                        AggregateViewModel.class,
                        () ->
                                new AggregateViewModel(
                                        fragment.getActivity().getApplication(),
                                        getTransferManager(),
                                        mProvisioner.getPersisterFactory(
                                                ApplicationContext.from(fragment)),
                                        mProvisioner.getWorkExecutor()));
            }
        };
    }

    public HistoryFragmentProvisioner getHistoryFragmentProvisioner(Fragment fragment) {
        return new HistoryFragmentProvisioner() {
            @MainThread
            @Override
            public TransferManager getTransferManager() {
                return mProvisioner.getTransferManager(ApplicationContext.from(fragment));
            }

            @Override
            public ViewModelProvider.Factory getViewModelFactory() {
                return mProvisioner.getViewModelFactory(
                        HistoryViewModel.class,
                        () ->
                                new HistoryViewModel(
                                        fragment.getActivity().getApplication(),
                                        getTransferManager(),
                                        mProvisioner.getWorkExecutor()));
            }

            @Override
            public HistoryAdapter getHistoryAdapter() {
                return mProvisioner.getHistoryAdapter(fragment);
            }
        };
    }

    public TransferServiceProvisioner getTransferServiceProvisioner(Service service) {
        return new TransferServiceProvisioner() {
            @Override
            public TaskManager getTaskManager() {
                return mProvisioner.getTaskManager(ApplicationContext.from(service));
            }
        };
    }

    public TransferManagerServiceProvisioner getTransferManagerServiceProvisioner(Service service) {
        return new TransferManagerServiceProvisioner() {
            @Override
            public TransferManager getTransferManager() {
                return mProvisioner.getTransferManager(ApplicationContext.from(service));
            }

            @Override
            public ExecutorService getRequestExecutor() {
                return mProvisioner.getRequestExecutor();
            }
        };
    }

    public CommandReceiverProvisioner getCommandReceiverProvisioner() {
        return new CommandReceiverProvisioner() {
            @Override
            public TransferManager getTransferManager(Context context) {
                return mProvisioner.getTransferManager(ApplicationContext.from(context));
            }

            @Override
            public ExecutorService getWorkExecutor() {
                return mProvisioner.getWorkExecutor();
            }

            @Override
            public ExecutorService getRequestExecutor() {
                return mProvisioner.getRequestExecutor();
            }
        };
    }
}
