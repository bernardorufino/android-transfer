package com.brufino.android.playground.extensions;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;
import android.util.Log;
import androidx.annotation.Nullable;
import com.brufino.android.common.CommonConstants;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.brufino.android.common.utils.Preconditions.checkArgument;

public class AndroidUtils {
    public static void time(String name, Runnable runnable) {
        long startTime = System.nanoTime();
        runnable.run();
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        Log.d("BMReq", name + " = " + time + " ms");
    }

    public static String getDebugCallback(Activity activity, String callback) {
        return String.format(
                Locale.US,
                "%s(%s).%s()",
                activity.getClass().getSimpleName(),
                Integer.toHexString(System.identityHashCode(activity)),
                callback);
    }

    public static void logCallback(Activity activity, String callback) {
        Log.d(CommonConstants.TAG, getDebugCallback(activity, callback));
    }

    @Nullable
    public static <T> T getSpecializedContext(Context context, Class<T> type) {
        while (true) {
            if (type.isInstance(context)) {
                return type.cast(context);
            }
            if (!(context instanceof ContextWrapper)) {
                break;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public static Path getDocumentPath(String name) {
        return Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toPath()
                .resolve(name);
    }

    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private AndroidUtils() {}
}
