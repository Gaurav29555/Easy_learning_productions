package com.fixcart.fixcart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateDispatchConfigurationRequest(
        @NotNull @Min(1) Long stalledMinutesThreshold,
        @NotNull @DecimalMin("0.10") Double regressionDistanceKm,
        @NotNull @Min(1) Long etaRegressionMinutes,
        @NotNull @DecimalMin("0.10") Double inactiveSpeedThresholdKmh
) {
}
