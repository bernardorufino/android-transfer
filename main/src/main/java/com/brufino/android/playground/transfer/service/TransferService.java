package com.brufino.android.playground.transfer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Process;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ComputableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.brufino.android.playground.R;
import com.brufino.android.playground.components.main.MainActivity;
import com.brufino.android.playground.extensions.NotificationUtils;
import com.brufino.android.playground.provision.Provisioners;
import com.brufino.android.playground.transfer.TransferManagerService;
import com.brufino.android.playground.transfer.TransferRequest;
import com.brufino.android.playground.transfer.task.TaskManager;
import com.brufino.android.playground.transfer.task.TransferTask;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.brufino.android.playground.extensions.concurrent.ConcurrencyUtils.resettingInterrupt;
import static com.brufino.android.playground.extensions.livedata.LiveDataUtils.computableLiveData;

public class TransferService extends TransferManagerService {
    private static final String THREAD_NAME = "transfer-service";
    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_INITIAL_TEXT = "In progress";

    private final MutableLiveData<Double> mLiveThroughput = new MutableLiveData<>();
    private final TransferServiceProvisioner mProvisioner;
    private ComputableLiveData<List<TransferRequest>> mLiveQueue;
    private NotificationManager mNotificationManager;
    private TaskManager mTaskManager;
    private Thread mServiceThread;

    /** Whenever mQueue is changed, you should call onQueueChanged(). */
    private BlockingQueue<TransferRequest> mQueue = new LinkedBlockingQueue<>();

    public TransferService() {
        mProvisioner = Provisioners.get().getTransferServiceProvisioner(this);
    }

    @Override
    public void onCreate() {
        mNotificationManager = getSystemService(NotificationManager.class);
        mTaskManager = mProvisioner.getTaskManager();
        mServiceThread = new Thread(this::work, THREAD_NAME);
        mServiceThread.start();
        mLiveQueue = computableLiveData(this::getQueue);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resettingInterrupt(mServiceThread::join);
        stopForeground(true);
    }

    public LiveData<List<TransferRequest>> getLiveQueue() {
        return mLiveQueue.getLiveData();
    }

    public LiveData<Double> getLiveThroughput() {
        return mLiveThroughput;
    }

    public void clearQueue() {
        mQueue.clear();
        onQueueChanged();
    }

    @Override
    protected void onStart() {
        startForeground(NOTIFICATION_ID, getNotification(NOTIFICATION_INITIAL_TEXT));
    }

    @Override
    protected void onHandleRequest(TransferRequest request) {
        if (!mQueue.offer(request)) {
            throw new AssertionError("Cannot insert element in queue");
        }
        onQueueChanged();
    }

    private void work() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            try {
                long startTime = System.nanoTime();
                TransferRequest request = mQueue.take();
                onQueueChanged();
                final TransferTask task;
                try {
                    task = mTaskManager.startTask(request.code, request.configuration);
                } catch (TaskManager.ConcurrentTaskException e) {
                    throw new IllegalStateException("A task was already running", e);
                }
                mNotificationManager.notify(
                        NOTIFICATION_ID,
                        getNotification("Work " + task.getClass().getSimpleName()));
                task.waitExecution();
                mLiveThroughput.postValue(getThroughput(startTime));
                if (tryStop(request)) {
                    break;
                }
            } catch (InterruptedException e) {
                stopSelf();
                break;
            }
        }
    }

    private List<TransferRequest> getQueue() {
        return new CopyOnWriteArrayList<>(mQueue);
    }

    private void onQueueChanged() {
        mLiveQueue.invalidate();
    }

    /** Number of tasks executed in one minute. */
    private double getThroughput(long startTimeNano) {
        return (double) TimeUnit.MINUTES.toNanos(1) / (System.nanoTime() - startTimeNano);
    }

    private Notification getNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);
        return new NotificationCompat.Builder(
                this,
                NotificationUtils.getChannel(this, NotificationUtils.CHANNEL_TRANSFER))
                .setContentTitle("Transfer")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_transfer_24dp)
                .setContentIntent(pendingIntent)
                .setTicker("Transfer in progress")
                .setSound(null)
                .build();
    }
}
