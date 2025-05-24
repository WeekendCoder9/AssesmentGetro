package com.trackingnumber.exception;

public class TrackingNumberException extends RuntimeException {
    public TrackingNumberException(String message) {
        super(message);
    }
    
    public TrackingNumberException(String message, Throwable cause) {
        super(message, cause);
    }
}
