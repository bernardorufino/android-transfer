package com.brufino.android.playground.extensions;

import android.content.Context;
import androidx.fragment.app.Fragment;

public class ApplicationContext {
    public static ApplicationContext from(Context context) {
        return new ApplicationContext(context.getApplicationContext());
    }

    public static ApplicationContext from(Fragment fragment) {
        return new ApplicationContext(fragment.getContext().getApplicationContext());
    }

    private final Context mContext;

    private ApplicationContext(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }
}
