package com.brufino.android.playground.extensions.livedata;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.brufino.android.common.CommonConstants;
import com.brufino.android.playground.extensions.ApplicationContext;

import static com.brufino.android.common.utils.Preconditions.checkNotNull;

public class LiveDataPersisterFactory {
    private final ApplicationContext mContext;

    public LiveDataPersisterFactory(ApplicationContext context) {
        mContext = context;
    }

    public LiveDataPersister getPersister(
            Class<? extends Activity> activityClass,
            String preferencesPrefix) {
        ComponentName component =
                ComponentName.createRelative(
                        mContext.getContext(),
                        checkNotNull(activityClass.getCanonicalName()));
        String name = component.getShortClassName().replaceFirst("^\\.", "");
        SharedPreferences preferences =
                mContext.getContext().getSharedPreferences(name, Context.MODE_PRIVATE);
        return new LiveDataPersister(mContext, preferences, preferencesPrefix);
    }
}
