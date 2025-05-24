package com.trackingnumber.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TrackingNumberResponse(
    @JsonProperty("tracking_number")
    String trackingNumber,
    
    @JsonProperty("created_at")
    String createdAt
) {}
