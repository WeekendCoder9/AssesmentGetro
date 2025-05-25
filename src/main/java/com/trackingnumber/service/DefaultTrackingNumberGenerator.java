package com.trackingnumber.service;

import com.trackingnumber.domain.TrackingNumberRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Component
public class DefaultTrackingNumberGenerator implements TrackingNumberGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTrackingNumberGenerator.class);
    private static final Pattern TRACKING_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9]{1,16}$");
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 16;
    private static final int DEFAULT_LENGTH = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String generate(TrackingNumberRequest request, int attempt) {
        if (request == null) {
            throw new IllegalArgumentException("TrackingNumberRequest cannot be null");
        }

        logger.debug("Generating tracking number for request: {}, attempt: {}", request, attempt);

        try {
            String input = buildInputString(request, attempt);
            String hash = generateHash(input);
            String trackingNumber = formatTrackingNumber(hash);

            if (!isValidTrackingNumber(trackingNumber)) {
                logger.warn("Generated invalid tracking number: {}", trackingNumber);
                throw new IllegalStateException("Generated tracking number does not match required pattern");
            }

            logger.debug("Generated tracking number: {}", trackingNumber);
            return trackingNumber;

        } catch (Exception e) {
            logger.error("Error generating tracking number for request: {}", request, e);
            throw new RuntimeException("Failed to generate tracking number", e);
        }
    }

    private String buildInputString(TrackingNumberRequest request, int attempt) {
        // Enhanced entropy sources
        long nanoTime = System.nanoTime();
        long currentTimeMillis = System.currentTimeMillis();
        int randomInt = ThreadLocalRandom.current().nextInt();
        byte[] randomBytes = new byte[8];
        SECURE_RANDOM.nextBytes(randomBytes);
        
        // Convert random bytes to hex string
        StringBuilder randomHex = new StringBuilder();
        for (byte b : randomBytes) {
            randomHex.append(String.format("%02x", b));
        }
        
        return String.format("%s|%s|%s|%s|%s|%s|%d|%d|%d|%d|%s",
                request.originCountryId(),
                request.destinationCountryId(),
                request.weight(),
                request.customerId(),
                request.customerName(),
                request.customerSlug() != null ? request.customerSlug() : "",
                attempt,
                nanoTime,
                currentTimeMillis,
                randomInt,
                randomHex.toString()
        );
    }

    private String generateHash(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(input.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }

    private String formatTrackingNumber(String hash) {
        if (hash == null || hash.isEmpty()) {
            throw new IllegalArgumentException("Hash cannot be null or empty");
        }

        StringBuilder result = new StringBuilder();
        int hashIndex = 0;
        
        // Use a more sophisticated approach to convert hash to alphanumeric
        while (result.length() < DEFAULT_LENGTH && hashIndex < hash.length()) {
            char c = hash.charAt(hashIndex);
            
            if (Character.isDigit(c)) {
                result.append(c);
            } else if (c >= 'A' && c <= 'F') {
                // Convert hex letters to valid letters (A-F maps to A-F, then continue with G-Z)
                if (c <= 'F') {
                    result.append(c);
                }
            } else {
                // For any other character, use modulo to get valid character
                int charValue = (int) c;
                if (charValue % 2 == 0) {
                    result.append((char) ('A' + (charValue % 26)));
                } else {
                    result.append((char) ('0' + (charValue % 10)));
                }
            }
            hashIndex++;
        }

        // If we still need more characters, use additional entropy
        while (result.length() < DEFAULT_LENGTH) {
            int randomChoice = SECURE_RANDOM.nextInt(36); // 0-35
            if (randomChoice < 10) {
                result.append((char) ('0' + randomChoice));
            } else {
                result.append((char) ('A' + (randomChoice - 10)));
            }
        }
        
        String trackingNumber = result.toString();
        
        // Ensure length constraints
        if (trackingNumber.length() > MAX_LENGTH) {
            trackingNumber = trackingNumber.substring(0, MAX_LENGTH);
        }
        
        // Ensure minimum length (should not happen with current logic, but safety check)
        while (trackingNumber.length() < MIN_LENGTH) {
            trackingNumber += "A";
        }

        return trackingNumber;
    }

    private boolean isValidTrackingNumber(String trackingNumber) {
        return trackingNumber != null &&
                trackingNumber.length() >= MIN_LENGTH &&
                trackingNumber.length() <= MAX_LENGTH &&
                TRACKING_NUMBER_PATTERN.matcher(trackingNumber).matches() &&
                !trackingNumber.trim().isEmpty();
    }
}
