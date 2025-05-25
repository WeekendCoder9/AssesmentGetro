package com.trackingnumber.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TrackingNumberRequest(
    @NotBlank(message = "Origin country ID is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Origin country ID must be in ISO 3166-1 alpha-2 format")
    String originCountryId,
    
    @NotBlank(message = "Destination country ID is required")  
    @Pattern(regexp = "^[A-Z]{2}$", message = "Destination country ID must be in ISO 3166-1 alpha-2 format")
    String destinationCountryId,
    
    @NotBlank(message = "Weight is required")
    @Pattern(regexp = "^\\d{1,3}\\.\\d{3}$", message = "Weight must be in format X.XXX (up to 3 decimal places)")
    String weight,
    
    @NotBlank(message = "Customer ID is required")
    @Size(max = 36, message = "Customer ID must not exceed 36 characters")
    String customerId,
    
    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Customer name must not exceed 100 characters") 
    String customerName,
    
    @Size(max = 50, message = "Customer slug must not exceed 50 characters")
    String customerSlug
) {}
