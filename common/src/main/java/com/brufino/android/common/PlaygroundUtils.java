package com.brufino.android.common;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.widget.Toast;

public class PlaygroundUtils {
    private static final String CONSUMER_ACTION = ConsumerService.ACTION;
    private static final String PRODUCER_ACTION = ProducerService.ACTION;

    public static Intent getConsumerIntent(String targetPackage) {
        Intent intent = new Intent(CONSUMER_ACTION);
        intent.setPackage(targetPackage);
        return intent;
    }

    public static Intent getProducerIntent(String targetPackage) {
        Intent intent = new Intent(PRODUCER_ACTION);
        intent.setPackage(targetPackage);
        return intent;
    }

    public static void postDebugToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(
                    context,
                    Process.myPid()
                            + " (" + Thread.currentThread().getName() + ")" + ": " + message,
                    Toast.LENGTH_SHORT)
                    .show();
        });
    }
}
