package com.brufino.android.playground.extensions.concurrent;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class AppThreadFactory implements java.util.concurrent.ThreadFactory {
    private final ThreadGroup mGroup;
    private final AtomicInteger mThreadNumber = new AtomicInteger(1);
    private final String mNameFormat;

    /** {@code nameFormat} will have %d replaced by thread number. */
    public AppThreadFactory(String nameFormat) {
        SecurityManager securityManager = System.getSecurityManager();
        mGroup =
                (securityManager != null)
                        ? securityManager.getThreadGroup()
                        : Thread.currentThread().getThreadGroup();
        mNameFormat = nameFormat;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = String.format(Locale.US, mNameFormat, mThreadNumber.getAndIncrement());
        Thread thread = new Thread(mGroup, runnable, name);
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }
}
