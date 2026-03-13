package com.fixcart.fixcart.dto;

public record DispatchConfigurationResponse(
        long stalledMinutesThreshold,
        double regressionDistanceKm,
        long etaRegressionMinutes,
        double inactiveSpeedThresholdKmh
) {
}
