package com.trackingnumber.exception;

public class DuplicateTrackingNumberException extends TrackingNumberException {
    public DuplicateTrackingNumberException(String trackingNumber) {
        super("Duplicate tracking number generated: " + trackingNumber);
    }
}
