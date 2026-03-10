package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.WorkerType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
        @NotNull WorkerType serviceType,
        @NotBlank String serviceAddress,
        @DecimalMin("-90.0") @DecimalMax("90.0") double customerLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double customerLongitude,
        String notes
) {
}
