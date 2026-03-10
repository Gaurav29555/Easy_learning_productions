package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.BookingStatus;
import com.fixcart.fixcart.entity.enums.WorkerType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long bookingId,
        Long customerId,
        Long workerId,
        WorkerType serviceType,
        String serviceAddress,
        double customerLatitude,
        double customerLongitude,
        LocalDateTime scheduledAt,
        String notes,
        BigDecimal estimatedPrice,
        String cancellationReason,
        BookingStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
