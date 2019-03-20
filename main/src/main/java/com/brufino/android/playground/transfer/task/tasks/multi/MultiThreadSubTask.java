package com.brufino.android.playground.transfer.task.tasks.multi;

import java.util.concurrent.CompletableFuture;

public interface MultiThreadSubTask extends Runnable {
    CompletableFuture<Void> getFuture();
}
