package com.fixcart.fixcart.dto;

public record AddressSuggestionResponse(
        String label,
        double latitude,
        double longitude,
        String regionCode,
        String provider
) {
}
