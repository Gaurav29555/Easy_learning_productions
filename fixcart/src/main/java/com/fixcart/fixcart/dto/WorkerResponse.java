package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.WorkerType;

public record WorkerResponse(
        Long workerId,
        Long userId,
        String fullName,
        WorkerType workerType,
        double latitude,
        double longitude,
        boolean available,
        double distanceKm
) {
}
