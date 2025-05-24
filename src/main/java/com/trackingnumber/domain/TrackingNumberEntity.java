package com.trackingnumber.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@RedisHash("tracking_numbers")
public record TrackingNumberEntity(
    @Id
    String trackingNumber,
    String createdAt,
    @TimeToLive
    long ttl
) {}
