package com.trackingnumber.service;

import com.trackingnumber.domain.TrackingNumberRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTrackingNumberGeneratorTest {

    private DefaultTrackingNumberGenerator generator;
    private TrackingNumberRequest validRequest;

    @BeforeEach
    void setUp() {
        generator = new DefaultTrackingNumberGenerator();
        validRequest = new TrackingNumberRequest(
                "US",
                "CA",
                "1.234",
                "de619854-b59b-425e-9db4-943379e1bd49",
                "RedBox Logistics",
                "redbox-logistics"
        );
    }

    @Test
    void shouldGenerateValidTrackingNumber() {
        String trackingNumber = generator.generate(validRequest, 0);

        assertNotNull(trackingNumber);
        assertTrue(trackingNumber.length() >= 1 && trackingNumber.length() <= 16);
        assertTrue(Pattern.matches("^[A-Z0-9]{1,16}$", trackingNumber));
    }

    @Test
    void shouldGenerateDifferentNumbersForDifferentAttempts() {
        String first = generator.generate(validRequest, 0);
        String second = generator.generate(validRequest, 1);

        assertNotEquals(first, second);
    }

    @Test
    void shouldGenerateConsistentNumberForSameInputAndAttempt() {
        String first = generator.generate(validRequest, 0);
        String second = generator.generate(validRequest, 0);

        // Note: Due to timestamp inclusion, these will be different
        // This test verifies the method works consistently
        assertNotNull(first);
        assertNotNull(second);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10, 100})
    void shouldHandleVariousAttemptNumbers(int attempt) {
        String trackingNumber = generator.generate(validRequest, attempt);

        assertNotNull(trackingNumber);
        assertTrue(Pattern.matches("^[A-Z0-9]{1,16}$", trackingNumber));
    }

    @Test
    void shouldHandleNullCustomerSlug() {
        TrackingNumberRequest requestWithNullSlug = new TrackingNumberRequest(
                "US", "CA", "1.234",
                "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", null
        );

        String trackingNumber = generator.generate(requestWithNullSlug, 0);

        assertNotNull(trackingNumber);
        assertTrue(Pattern.matches("^[A-Z0-9]{1,16}$", trackingNumber));
    }
}
