package com.trackingnumber.performance;

import com.trackingnumber.domain.TrackingNumberRequest;
import com.trackingnumber.service.DefaultTrackingNumberGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TrackingNumberPerformanceTest {
    
    private final DefaultTrackingNumberGenerator generator = new DefaultTrackingNumberGenerator();
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldGenerateTrackingNumbersQuickly() {
        TrackingNumberRequest request = new TrackingNumberRequest(
            "US", "CA", "1.234", "2018-11-20T19:29:32+08:00",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            String trackingNumber = generator.generate(request, i);
            assertNotNull(trackingNumber);
            assertTrue(trackingNumber.length() <= 16);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should generate 1000 tracking numbers in less than 10 seconds
        assertTrue(duration < 10000, "Generation took too long: " + duration + "ms");
    }
    
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentGeneration() throws InterruptedException {
        TrackingNumberRequest request = new TrackingNumberRequest(
            "US", "CA", "1.234", "2018-11-20T19:29:32+08:00",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
        
        int threadCount = 10;
        int generationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<String> generatedNumbers = ConcurrentHashMap.newKeySet();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < generationsPerThread; j++) {
                        String trackingNumber = generator.generate(request, threadId * generationsPerThread + j);
                        generatedNumbers.add(trackingNumber);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        
        // Verify all generated numbers are unique (high probability)
        assertEquals(threadCount * generationsPerThread, generatedNumbers.size());
    }
}
