package com.brufino.android.playground.extensions.concurrent;

import android.os.Handler;

class LoopingRunnable implements Runnable {
    private final Handler mHandler;
    private final Runnable mRunnable;
    private final long mInterval;

    LoopingRunnable(Handler handler, Runnable runnable, long intervalMs) {
        mHandler = handler;
        mRunnable = runnable;
        mInterval = intervalMs;
    }

    @Override
    public void run() {
        mRunnable.run();
        mHandler.postDelayed(this, mInterval);
    }
}
