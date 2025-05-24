package com.trackingnumber.service;

import com.trackingnumber.domain.TrackingNumberRequest;

public interface TrackingNumberGenerator {
    String generate(TrackingNumberRequest request, int attempt);
}
