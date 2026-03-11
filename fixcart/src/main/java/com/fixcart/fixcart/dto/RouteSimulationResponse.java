package com.fixcart.fixcart.dto;

import java.util.List;

public record RouteSimulationResponse(
        Long bookingId,
        double originLatitude,
        double originLongitude,
        double destinationLatitude,
        double destinationLongitude,
        double totalDistanceKm,
        long etaMinutes,
        List<RoutePointResponse> routePoints
) {
}
