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
import org.springframework.data.redis.core.ReactiveRedisTemplate;
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
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final int maxRetries;
    private final long ttlSeconds;

    public TrackingNumberServiceImpl(
            TrackingNumberRepository repository,
            TrackingNumberGenerator generator,
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Value("${tracking-number.max-retries:10}") int maxRetries,
            @Value("${tracking-number.ttl-seconds:86400}") long ttlSeconds) {
        this.repository = repository;
        this.generator = generator;
        this.redisTemplate = redisTemplate;
        this.maxRetries = maxRetries;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    @NewSpan("generate-unique-tracking-number")
    public Mono<String> generateUniqueTrackingNumber(TrackingNumberRequest request) {
        if (request == null) {
            return Mono.error(new TrackingNumberException("TrackingNumberRequest cannot be null"));
        }

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

        String candidateNumber;
        try {
            candidateNumber = generator.generate(request, attempt);
            if (candidateNumber == null || candidateNumber.trim().isEmpty()) {
                throw new TrackingNumberException("Generated tracking number is null or empty");
            }
        } catch (Exception e) {
            logger.error("Error generating tracking number candidate on attempt {}: {}", attempt + 1, e.getMessage());
            return Mono.error(new TrackingNumberException("Failed to generate tracking number", e));
        }

        return atomicCheckAndStore(candidateNumber)
                .then(Mono.just(candidateNumber))
                .onErrorResume(DuplicateTrackingNumberException.class,
                        ex -> {
                            logger.warn("Duplicate tracking number detected: {}, retrying (attempt {}/{})", 
                                      candidateNumber, attempt + 1, maxRetries);
                            return generateWithRetry(request, attempt + 1);
                        })
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .filter(throwable -> !(throwable instanceof DuplicateTrackingNumberException))
                        .filter(throwable -> !(throwable instanceof TrackingNumberException))
                        .doBeforeRetry(retrySignal -> 
                                logger.debug("Retrying due to transient error: {}", retrySignal.failure().getMessage())));
    }

    private Mono<Void> atomicCheckAndStore(String trackingNumber) {
        logger.debug("Atomically checking and storing tracking number: {}", trackingNumber);

        // Use Redis SETNX (SET if Not eXists) for atomic check-and-set
        String redisKey = "tracking_number:" + trackingNumber;
        String timestamp = Instant.now().toString();
        
        return redisTemplate.opsForValue()
                .setIfAbsent(redisKey, timestamp)
                .flatMap(wasSet -> {
                    if (!wasSet) {
                        logger.debug("Tracking number already exists: {}", trackingNumber);
                        return Mono.error(new DuplicateTrackingNumberException(trackingNumber));
                    }
                    
                    // Set TTL for the Redis key
                    return redisTemplate.expire(redisKey, Duration.ofSeconds(ttlSeconds))
                            .then(saveToRepository(trackingNumber, timestamp));
                })
                .onErrorMap(throwable -> {
                    if (throwable instanceof DuplicateTrackingNumberException) {
                        return throwable;
                    }
                    logger.error("Error in atomic check-and-store for tracking number: {}", trackingNumber, throwable);
                    return new TrackingNumberException("Failed to store tracking number: " + trackingNumber, throwable);
                });
    }

    private Mono<Void> saveToRepository(String trackingNumber, String timestamp) {
        TrackingNumberEntity entity = new TrackingNumberEntity(
                trackingNumber,
                timestamp,
                ttlSeconds
        );

        return repository.save(entity)
                .doOnSuccess(savedEntity -> 
                        logger.debug("Successfully stored tracking number in repository: {}", trackingNumber))
                .then()
                .onErrorMap(throwable -> {
                    logger.error("Error saving to repository for tracking number: {}", trackingNumber, throwable);
                    return new TrackingNumberException("Failed to save tracking number to repository: " + trackingNumber, throwable);
                });
    }
}
