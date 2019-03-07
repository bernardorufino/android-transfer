package com.brufino.android.playground.extensions.service;

import android.content.Intent;
import android.os.IBinder;
import com.brufino.android.playground.extensions.ApplicationContext;

import java.util.function.Function;

public class ServiceClientFactory {
    private final ApplicationContext mContext;

    public ServiceClientFactory(ApplicationContext context) {
        mContext = context;
    }

    public <T> ServiceClient<T> getServiceClient(
            Intent serviceIntent,
            Function<IBinder, T> converter) {
        return new ServiceClient<>(mContext, serviceIntent, converter);
    }
}
