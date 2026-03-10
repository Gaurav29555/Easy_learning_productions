package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBookingStatusRequest(
        @NotNull BookingStatus status,
        String cancellationReason
) {
}
