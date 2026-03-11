package com.fixcart.fixcart.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminMetricsResponse(
        long totalCustomers,
        long totalWorkers,
        long availableWorkers,
        long totalBookings,
        long completedBookings,
        long cancelledBookings,
        long successfulPayments,
        long activeBookings,
        long pendingWorkerApprovals,
        BigDecimal grossMerchandiseValue,
        BigDecimal averageOrderValue,
        double completionRate,
        double cancellationRate,
        double workerUtilizationRate,
        double assignmentRate,
        List<ServiceDemandMetric> topServiceDemand
) {
}
