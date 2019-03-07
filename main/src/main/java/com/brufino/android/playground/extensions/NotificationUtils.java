package com.brufino.android.playground.extensions;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

public class NotificationUtils {
    public static final NotificationChannel CHANNEL_TRANSFER =
            new NotificationChannel(
                    "transfer", "Transfer", NotificationManager.IMPORTANCE_LOW);

    public static String getChannel(Context context, NotificationChannel channel) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
        return channel.getId();
    }

}
