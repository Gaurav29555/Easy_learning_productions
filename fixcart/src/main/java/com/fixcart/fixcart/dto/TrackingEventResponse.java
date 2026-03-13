package com.fixcart.fixcart.dto;

import java.time.LocalDateTime;

public record TrackingEventResponse(
        Long bookingId,
        Long workerId,
        double latitude,
        double longitude,
        double speedKmh,
        double distanceToDestinationKm,
        long etaMinutes,
        double routeConfidenceScore,
        String inactivityWarning,
        LocalDateTime createdAt
) {
}
