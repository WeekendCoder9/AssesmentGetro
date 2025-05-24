package com.trackingnumber.service;

import com.trackingnumber.domain.TrackingNumberEntity;
import com.trackingnumber.domain.TrackingNumberRequest;
import com.trackingnumber.exception.DuplicateTrackingNumberException;
import com.trackingnumber.exception.TrackingNumberException;
import com.trackingnumber.repository.TrackingNumberRepository;
import io.micrometer.tracing.annotation.NewSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;

@Service
public class TrackingNumberServiceImpl implements TrackingNumberService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingNumberServiceImpl.class);

    private final TrackingNumberRepository repository;
    private final TrackingNumberGenerator generator;
    private final int maxRetries;
    private final long ttlSeconds;

    public TrackingNumberServiceImpl(
            TrackingNumberRepository repository,
            TrackingNumberGenerator generator,
            @Value("${tracking-number.max-retries:10}") int maxRetries,
            @Value("${tracking-number.ttl-seconds:86400}") long ttlSeconds) {
        this.repository = repository;
        this.generator = generator;
        this.maxRetries = maxRetries;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    @NewSpan("generate-unique-tracking-number")
    public Mono<String> generateUniqueTrackingNumber(TrackingNumberRequest request) {
        logger.info("Starting tracking number generation for customer: {}", request.customerId());

        return generateWithRetry(request, 0)
                .doOnSuccess(trackingNumber ->
                        logger.info("Successfully generated tracking number: {} for customer: {}",
                                trackingNumber, request.customerId()))
                .doOnError(error ->
                        logger.error("Failed to generate tracking number for customer: {}",
                                request.customerId(), error));
    }

    private Mono<String> generateWithRetry(TrackingNumberRequest request, int attempt) {
        if (attempt >= maxRetries) {
            logger.error("Max retries exceeded for tracking number generation. Customer: {}",
                    request.customerId());
            return Mono.error(new TrackingNumberException(
                    "Failed to generate unique tracking number after " + maxRetries + " attempts"));
        }

        logger.debug("Tracking number generation attempt {} for customer: {}",
                attempt + 1, request.customerId());

        String candidateNumber = generator.generate(request, attempt);

        return checkAndStoreTrackingNumber(candidateNumber)
                .then(Mono.just(candidateNumber))
                .onErrorResume(DuplicateTrackingNumberException.class,
                        ex -> {
                            logger.warn("Duplicate tracking number detected: {}, retrying...", candidateNumber);
                            return generateWithRetry(request, attempt + 1);
                        })
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .filter(throwable -> !(throwable instanceof DuplicateTrackingNumberException)));
    }

    private Mono<Void> checkAndStoreTrackingNumber(String trackingNumber) {
        logger.debug("Checking uniqueness and storing tracking number: {}", trackingNumber);

        TrackingNumberEntity entity = new TrackingNumberEntity(
                trackingNumber,
                Instant.now().toString(),
                ttlSeconds
        );

        return repository.existsById(trackingNumber)
                .flatMap(exists -> {
                    if (exists) {
                        logger.debug("Tracking number already exists: {}", trackingNumber);
                        return Mono.error(new DuplicateTrackingNumberException(trackingNumber));
                    }
                    return repository.save(entity).then();
                })
                .doOnSuccess(unused ->
                        logger.debug("Successfully stored tracking number: {}", trackingNumber))
                .doOnError(error ->
                        logger.error("Error storing tracking number: {}", trackingNumber, error));
    }
}
