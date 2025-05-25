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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TrackingNumberPerformanceTest {
    
    private final DefaultTrackingNumberGenerator generator = new DefaultTrackingNumberGenerator();
    
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldGenerateTrackingNumbersQuickly() {
        TrackingNumberRequest request = new TrackingNumberRequest(
            "US", "CA", "1.234",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            String trackingNumber = generator.generate(request, i);
            assertNotNull(trackingNumber);
            assertTrue(trackingNumber.length() >= 1 && trackingNumber.length() <= 16);
            assertTrue(trackingNumber.matches("^[A-Z0-9]{1,16}$"));
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should generate 1000 tracking numbers in less than 5 seconds
        assertTrue(duration < 5000, "Generation took too long: " + duration + "ms");
        System.out.println("Generated 1000 tracking numbers in " + duration + "ms");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentGeneration() throws InterruptedException {
        TrackingNumberRequest request = new TrackingNumberRequest(
            "US", "CA", "1.234",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
        
        int threadCount = 10;
        int generationsPerThread = 50; // Reduced for faster test execution
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<String> generatedNumbers = ConcurrentHashMap.newKeySet();
        AtomicInteger totalGenerated = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < generationsPerThread; j++) {
                        String trackingNumber = generator.generate(request, threadId * generationsPerThread + j);
                        assertNotNull(trackingNumber);
                        assertTrue(trackingNumber.matches("^[A-Z0-9]{1,16}$"));
                        generatedNumbers.add(trackingNumber);
                        totalGenerated.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Exception during concurrent generation: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(15, TimeUnit.SECONDS), "Concurrent generation timed out");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor shutdown timed out");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify all threads completed successfully
        assertEquals(threadCount * generationsPerThread, totalGenerated.get());
        
        // Verify high uniqueness rate (should be close to 100% unique)
        int expectedTotal = threadCount * generationsPerThread;
        double uniquenessRate = (double) generatedNumbers.size() / expectedTotal;
        
        System.out.println("Generated " + totalGenerated.get() + " tracking numbers in " + duration + "ms");
        System.out.println("Unique numbers: " + generatedNumbers.size() + "/" + expectedTotal + 
                          " (" + String.format("%.2f", uniquenessRate * 100) + "% unique)");
        
        // We expect very high uniqueness due to different attempt numbers
        assertTrue(uniquenessRate > 0.95, "Uniqueness rate too low: " + uniquenessRate);
    }
    
    @Test
    void shouldGenerateValidFormats() {
        TrackingNumberRequest request = new TrackingNumberRequest(
            "US", "CA", "1.234",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
        
        // Test various scenarios
        for (int i = 0; i < 100; i++) {
            String trackingNumber = generator.generate(request, i);
            
            assertNotNull(trackingNumber, "Tracking number should not be null");
            assertFalse(trackingNumber.isEmpty(), "Tracking number should not be empty");
            assertTrue(trackingNumber.length() <= 16, "Tracking number too long: " + trackingNumber);
            assertTrue(trackingNumber.matches("^[A-Z0-9]+$"), "Invalid characters in: " + trackingNumber);
        }
    }
    
    @Test
    void shouldHandleDifferentInputs() {
        TrackingNumberRequest[] requests = {
            new TrackingNumberRequest("US", "CA", "1.000", "customer1", "Customer One", "customer-one"),
            new TrackingNumberRequest("GB", "DE", "999.999", "customer2", "Customer Two", null),
            new TrackingNumberRequest("FR", "IT", "500.500", "customer3", "Customer Three", "customer-three"),
            new TrackingNumberRequest("AU", "JP", "0.001", "customer4", "Customer Four", "customer-four")
        };
        
        for (TrackingNumberRequest request : requests) {
            String trackingNumber = generator.generate(request, 0);
            assertNotNull(trackingNumber);
            assertTrue(trackingNumber.matches("^[A-Z0-9]{1,16}$"));
        }
    }
}
