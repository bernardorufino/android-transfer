package com.brufino.android.playground.extensions.livedata;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.extensions.Converter;
import com.brufino.android.playground.extensions.livedata.LiveDataUtils;

import java.util.Locale;
import java.util.Set;

public class LiveDataPersister {
    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final String mPreferencesPrefix;

    LiveDataPersister(
            ApplicationContext context,
            SharedPreferences preferences,
            String preferencesPrefix) {
        mContext = context.getContext();
        mPreferences = preferences;
        mPreferencesPrefix = preferencesPrefix;
    }

    public <T> void persist(
            LifecycleOwner owner,
            MutableLiveData<T> liveData,
            String preferenceName,
            T defaultValue) {
        persist(owner, liveData, prefix(preferenceName), defaultValue, Converter.identity());
    }

    /** U has to be persistable. */
    @SuppressLint("CommitPrefEdits")
    private <T, U> void persist(
            LifecycleOwner owner,
            MutableLiveData<T> liveData,
            String preferenceName,
            T defaultValue,
            Converter<T, U> converter) {
        // When owner is destroyed observer is automatically removed from liveData
        liveData.observe(
                owner,
                value ->
                        putPreference(
                                mPreferences.edit(),
                                preferenceName,
                                converter.convert(value))
                                .apply());
        LiveDataUtils.setValue(
                liveData,
                converter.revert(
                        getPreference(
                                mPreferences, preferenceName, converter.convert(defaultValue))));

    }

    private String prefix(String preferenceName) {
        return String.format(Locale.US, "%s:%s", mPreferencesPrefix, preferenceName);
    }

    private static <T> T getPreference(SharedPreferences preferences, String key, T defaultValue) {
        @SuppressWarnings("unchecked")
        Class<? extends T> type = (Class<? extends T>) defaultValue.getClass();
        if (defaultValue instanceof Integer) {
            return type.cast(preferences.getInt(key, (Integer) defaultValue));
        }
        if (defaultValue instanceof Boolean) {
            return type.cast(preferences.getBoolean(key, (Boolean) defaultValue));
        }
        if (defaultValue instanceof Float) {
            return type.cast(preferences.getFloat(key, (Float) defaultValue));
        }
        if (defaultValue instanceof Long) {
            return type.cast(preferences.getLong(key, (Long) defaultValue));
        }
        if (defaultValue instanceof String) {
            return type.cast(preferences.getString(key, (String) defaultValue));
        }
        if (defaultValue instanceof Set) {
            //noinspection unchecked
            return type.cast(preferences.getStringSet(key, (Set<String>) defaultValue));
        }
        throw new IllegalArgumentException("Unsupported type " + defaultValue.getClass());
    }

    private static <T> SharedPreferences.Editor putPreference(
            SharedPreferences.Editor editor, String key, T value) {
        if (value instanceof Integer) {
            return editor.putInt(key, (Integer) value);
        }
        if (value instanceof Boolean) {
            return editor.putBoolean(key, (Boolean) value);
        }
        if (value instanceof Float) {
            return editor.putFloat(key, (Float) value);
        }
        if (value instanceof Long) {
            return editor.putLong(key, (Long) value);
        }
        if (value instanceof String) {
            return editor.putString(key, (String) value);
        }
        if (value instanceof Set) {
            //noinspection unchecked
            return editor.putStringSet(key, (Set<String>) value);
        }
        throw new IllegalArgumentException("Unsupported type " + value.getClass());
    }
}
