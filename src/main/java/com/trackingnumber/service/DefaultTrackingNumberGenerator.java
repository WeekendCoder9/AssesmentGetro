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

    @Override
    public String generate(TrackingNumberRequest request, int attempt) {
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
        // Use current date instead of timestamp for better date-based generation
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        
        return String.format("%s_%s_%s_%s_%s_%s_%s_%d",
                request.originCountryId(),
                request.destinationCountryId(),
                request.weight(),
                request.customerId(),
                request.customerName(),
                request.customerSlug() != null ? request.customerSlug() : "",
                currentDate,
                attempt
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
        // Take first 10 characters and ensure it matches pattern requirements
        String candidate = hash.substring(0, Math.min(10, hash.length()));

        // Replace any invalid characters with valid ones
        StringBuilder result = new StringBuilder();
        for (char c : candidate.toCharArray()) {
            if (Character.isDigit(c) || (c >= 'A' && c <= 'Z')) {
                result.append(c);
            } else {
                // Convert hex characters (A-F are already valid, but handle others)
                if (c >= 'a' && c <= 'f') {
                    result.append(Character.toUpperCase(c));
                } else {
                    // Convert to valid character using modulo
                    result.append((char) ('A' + (c % 26)));
                }
            }
        }

        return result.toString();
    }

    private boolean isValidTrackingNumber(String trackingNumber) {
        return trackingNumber != null &&
                trackingNumber.length() >= 1 &&
                trackingNumber.length() <= 16 &&
                TRACKING_NUMBER_PATTERN.matcher(trackingNumber).matches();
    }
}
