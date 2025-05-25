package com.trackingnumber.service;

import com.trackingnumber.domain.TrackingNumberRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Component
public class DefaultTrackingNumberGenerator implements TrackingNumberGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTrackingNumberGenerator.class);
    private static final Pattern TRACKING_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9]{1,16}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 16;
    private static final int DEFAULT_LENGTH = 10;

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
        // Use current date for better date-based generation
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        
        // Add some entropy with system nanotime to reduce collisions
        long nanoTime = System.nanoTime();
        
        return String.format("%s_%s_%s_%s_%s_%s_%s_%d_%d",
                request.originCountryId(),
                request.destinationCountryId(),
                request.weight(),
                request.customerId(),
                request.customerName(),
                request.customerSlug() != null ? request.customerSlug() : "",
                currentDate,
                attempt,
                nanoTime % 10000 // Use last 4 digits for additional entropy
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

        // Take first DEFAULT_LENGTH characters and ensure it matches pattern requirements
        int lengthToTake = Math.min(DEFAULT_LENGTH, hash.length());
        String candidate = hash.substring(0, lengthToTake);

        // Convert to valid alphanumeric characters
        StringBuilder result = new StringBuilder();
        for (char c : candidate.toCharArray()) {
            if (Character.isDigit(c) || (c >= 'A' && c <= 'Z')) {
                result.append(c);
            } else if (c >= 'a' && c <= 'z') {
                // Convert lowercase to uppercase
                result.append(Character.toUpperCase(c));
            } else {
                // Convert any other character to valid character using modulo
                int charCode = (int) c;
                if (charCode % 2 == 0) {
                    result.append((char) ('A' + (charCode % 26)));
                } else {
                    result.append((char) ('0' + (charCode % 10)));
                }
            }
        }

        String trackingNumber = result.toString();
        
        // Ensure minimum length
        while (trackingNumber.length() < MIN_LENGTH) {
            trackingNumber += "A";
        }
        
        // Ensure maximum length
        if (trackingNumber.length() > MAX_LENGTH) {
            trackingNumber = trackingNumber.substring(0, MAX_LENGTH);
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
