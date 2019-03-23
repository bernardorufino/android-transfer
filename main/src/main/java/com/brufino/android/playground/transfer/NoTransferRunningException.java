package com.brufino.android.playground.transfer;

public class NoTransferRunningException extends Exception {
    public NoTransferRunningException() {}

    public NoTransferRunningException(String message) {
        super(message);
    }

    public NoTransferRunningException(String message, Throwable cause) {
        super(message, cause);
    }
}
