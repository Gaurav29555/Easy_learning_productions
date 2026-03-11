package com.fixcart.fixcart.dto;

public record BookingRealtimeEvent(
        String eventType,
        String message,
        BookingResponse booking
) {
}
