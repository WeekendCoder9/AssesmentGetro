package com.trackingnumber.service;

import com.trackingnumber.domain.TrackingNumberRequest;
import reactor.core.publisher.Mono;

public interface TrackingNumberService {
    Mono<String> generateUniqueTrackingNumber(TrackingNumberRequest request);
}
