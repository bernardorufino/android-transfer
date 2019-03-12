package com.brufino.android.playground.extensions.service;

import android.content.Intent;
import android.os.IBinder;
import com.brufino.android.playground.extensions.ApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class ServiceClientFactory {
    private final ApplicationContext mContext;
    private final ExecutorService mWorkExecutor;

    public ServiceClientFactory(ApplicationContext context, ExecutorService workExecutor) {
        mContext = context;
        mWorkExecutor = workExecutor;
    }

    public <T> ServiceClient<T> getServiceClient(
            Intent serviceIntent,
            Function<IBinder, T> converter) {
        return new ServiceClient<>(mContext, mWorkExecutor, serviceIntent, converter);
    }
}
