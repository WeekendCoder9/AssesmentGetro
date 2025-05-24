package com.trackingnumber.controller;

import com.trackingnumber.domain.TrackingNumberRequest;
import com.trackingnumber.domain.TrackingNumberResponse;
import com.trackingnumber.service.TrackingNumberService;
import io.micrometer.tracing.annotation.NewSpan;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
public class TrackingNumberController {

    private static final Logger logger = LoggerFactory.getLogger(TrackingNumberController.class);

    private final TrackingNumberService trackingNumberService;

    public TrackingNumberController(TrackingNumberService trackingNumberService) {
        this.trackingNumberService = trackingNumberService;
    }

    @PostMapping("/next-tracking-number")
    @ResponseStatus(HttpStatus.CREATED)
    @NewSpan("get-next-tracking-number")
    public Mono<TrackingNumberResponse> getNextTrackingNumber(
            @Valid @RequestBody TrackingNumberRequest request) {

        logger.info("Received tracking number request from customer: {} ({})",
                request.customerName(), request.customerId());
        logger.debug("Request details: {}", request);

        return trackingNumberService.generateUniqueTrackingNumber(request)
                .map(trackingNumber -> new TrackingNumberResponse(
                        trackingNumber,
                        Instant.now().toString()
                ))
                .doOnSuccess(response ->
                        logger.info("Successfully generated tracking number response: {}", response.trackingNumber()))
                .doOnError(error ->
                        logger.error("Error processing tracking number request for customer: {}",
                                request.customerId(), error));
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}
