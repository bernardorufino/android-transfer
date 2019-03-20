package com.brufino.android.playground.extensions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Testing {
    public static void main(String[] args) {
        CompletableFuture<Object> f1 =
                CompletableFuture.supplyAsync(
                        () -> {
                            throw new RuntimeException("1");
                        });
        CompletableFuture<Void> f2 = new CompletableFuture<>();
        f2.completeExceptionally(new RuntimeException("2"));

        try {
            f1
                    .whenCompleteAsync((v, t) -> System.out.println("whenComplete1(t = " + t + ")"))
                    //=> whenComplete1(t = CompletionException(cause = RE(1))
                    .whenCompleteAsync((v, t) -> System.out.println("whenComplete2(t = " + t + ")"))
                    //=> whenComplete2(t = CompletionException(cause = RE(1))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("get() = " + e);
            //=> ExecutionException(cause = RE(1))
        }

        try {
            f2
                    .whenCompleteAsync((v, t) -> System.out.println("whenComplete1(t = " + t + ")"))
                    //=> whenComplete1(t = RE(2))
                    .whenCompleteAsync((v, t) -> System.out.println("whenComplete2(t = " + t + ")"))
                    //=> whenComplete2(t = CompletionException(cause = RE(2)))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("get() = " + e);
            //=> ExecutionException(cause = RE(2))
        }

        try {
            f1
                    .handleAsync((v, t) -> {
                        System.out.println("handleAsync(t = " + t + ")");
                        //=> handleAsync(t = CompletionException(cause = RE(1))
                        throw new RuntimeException("HANDLE-1");
                    })
                    .whenCompleteAsync((v, t) -> System.out.println("whenComplete(t = " + t + ")"))
                    //=> whenComplete(t = CompletionException(cause = RE(HANDLE-1))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("get() = " + e);
            //=> ExecutionException(cause = RE(HANDLE-1))
        }

        try {
            f2
                    .handleAsync((v, t) -> {
                        System.out.println("handleAsync(t = " + t + ")");
                        //=> handleAsync(t = RE(1))
                        throw new RuntimeException("HANDLE-2");
                    })
                    .whenCompleteAsync((v, t) -> System.out.println("whenComplete(t = " + t + ")"))
                    //=> whenComplete(t = CompletionException(cause = RE(HANDLE-2))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("get() = " + e);
            //=> ExecutionException(cause = RE(HANDLE-2))
        }

        try {
            f1
                    .whenCompleteAsync((v, t) -> System.out.println("whenComplete1(t = " + t + ")"))
                    //=> whenComplete1(t = CompletionException(cause = RE(1))
                    .whenCompleteAsync((v, t) -> System.out.println("whenComplete2(t = " + t + ")"))
                    //=> whenComplete2(t = CompletionException(cause = RE(1))
                    .join();
        } catch (Exception e) {
            System.out.println("join() = " + e);
            //=> CompletionException(cause = RE(1))
        }

        try {
            f2
                    .whenCompleteAsync((v, t) -> System.out.println("whenComplete1(t = " + t + ")"))
                    //=> whenComplete1(t = RE(2))
                    .whenCompleteAsync((v, t) -> System.out.println("whenComplete2(t = " + t + ")"))
                    //=> whenComplete2(t = CompletionException(cause = RE(2)))
                    .join();
        } catch (Exception e) {
            System.out.println("join() = " + e);
            //=> CompletionException(cause = RE(2))
        }

        try {
            f1.get();
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("f1.get() = "+ e);
            //=> ExecutionException(cause = RE(1))
        }

        try {
            f2.get();
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("f2.get() = "+ e);
            //=> ExecutionException(cause = RE(2))
        }

        try {
            f1.join();
        } catch (Exception e) {
            System.out.println("f1.join() = "+ e);
            //=> CompletionException(cause = RE(1))
        }

        try {
            f2.join();
        } catch (Exception e) {
            System.out.println("f2.join() = "+ e);
            //=> CompletionException(cause = RE(2))
        }
    }
}
