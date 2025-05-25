package com.trackingnumber.service;

import com.trackingnumber.domain.TrackingNumberEntity;
import com.trackingnumber.domain.TrackingNumberRequest;
import com.trackingnumber.exception.DuplicateTrackingNumberException;
import com.trackingnumber.exception.TrackingNumberException;
import com.trackingnumber.repository.TrackingNumberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingNumberServiceImplTest {
    
    @Mock
    private TrackingNumberRepository repository;
    
    @Mock
    private TrackingNumberGenerator generator;
    
    private TrackingNumberServiceImpl service;
    private TrackingNumberRequest validRequest;
    
    @BeforeEach
    void setUp() {
        service = new TrackingNumberServiceImpl(repository, generator, 10, 86400);
        validRequest = new TrackingNumberRequest(
            "US", "CA", "1.234", "2018-11-20T19:29:32+08:00",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
    }
    
    @Test
    void shouldGenerateUniqueTrackingNumber() {
        String expectedTrackingNumber = "ABC123DEF4";
        
        when(generator.generate(any(TrackingNumberRequest.class), anyInt()))
            .thenReturn(expectedTrackingNumber);
        when(repository.existsById(expectedTrackingNumber))
            .thenReturn(Mono.just(false));
        when(repository.save(any(TrackingNumberEntity.class)))
            .thenReturn(Mono.just(new TrackingNumberEntity(expectedTrackingNumber, "2025-05-24T00:00:00Z", 86400)));
        
        StepVerifier.create(service.generateUniqueTrackingNumber(validRequest))
            .expectNext(expectedTrackingNumber)
            .verifyComplete();
        
        verify(generator).generate(validRequest, 0);
        verify(repository).existsById(expectedTrackingNumber);
        verify(repository).save(any(TrackingNumberEntity.class));
    }
    
    @Test
    void shouldRetryOnDuplicateTrackingNumber() {
        String duplicateNumber = "ABC123DEF4";
        String uniqueNumber = "XYZ789GHI0";
        
        when(generator.generate(any(TrackingNumberRequest.class), eq(0)))
            .thenReturn(duplicateNumber);
        when(generator.generate(any(TrackingNumberRequest.class), eq(1)))
            .thenReturn(uniqueNumber);
        
        when(repository.existsById(duplicateNumber))
            .thenReturn(Mono.just(true));
        when(repository.existsById(uniqueNumber))
            .thenReturn(Mono.just(false));
        when(repository.save(any(TrackingNumberEntity.class)))
            .thenReturn(Mono.just(new TrackingNumberEntity(uniqueNumber, "2025-05-24T00:00:00Z", 86400)));
        
        StepVerifier.create(service.generateUniqueTrackingNumber(validRequest))
            .expectNext(uniqueNumber)
            .verifyComplete();
        
        verify(generator).generate(validRequest, 0);
        verify(generator).generate(validRequest, 1);
        verify(repository).existsById(duplicateNumber);
        verify(repository).existsById(uniqueNumber);
    }
    
    @Test
    void shouldFailAfterMaxRetries() {
        String duplicateNumber = "ABC123DEF4";
        
        when(generator.generate(any(TrackingNumberRequest.class), anyInt()))
            .thenReturn(duplicateNumber);
        when(repository.existsById(duplicateNumber))
            .thenReturn(Mono.just(true));
        
        StepVerifier.create(service.generateUniqueTrackingNumber(validRequest))
            .expectError(TrackingNumberException.class)
            .verify();
        
        verify(generator, times(10)).generate(eq(validRequest), anyInt());
    }
    
    @Test
    void shouldHandleRepositoryError() {
        String trackingNumber = "ABC123DEF4";
        
        when(generator.generate(any(TrackingNumberRequest.class), anyInt()))
            .thenReturn(trackingNumber);
        when(repository.existsById(trackingNumber))
            .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));
        
        StepVerifier.create(service.generateUniqueTrackingNumber(validRequest))
            .expectError(RuntimeException.class)
            .verify();
    }
}
