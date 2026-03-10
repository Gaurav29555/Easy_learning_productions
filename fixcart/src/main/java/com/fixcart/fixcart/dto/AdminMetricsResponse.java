package com.fixcart.fixcart.dto;

public record AdminMetricsResponse(
        long totalCustomers,
        long totalWorkers,
        long availableWorkers,
        long totalBookings,
        long completedBookings,
        long cancelledBookings,
        long successfulPayments
) {
}
