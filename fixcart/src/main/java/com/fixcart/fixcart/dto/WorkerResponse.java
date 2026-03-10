package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.WorkerType;
import com.fixcart.fixcart.entity.enums.WorkerApprovalStatus;

public record WorkerResponse(
        Long workerId,
        Long userId,
        String fullName,
        WorkerType workerType,
        WorkerApprovalStatus approvalStatus,
        String kycDocumentUrl,
        int yearsOfExperience,
        double latitude,
        double longitude,
        boolean available,
        double distanceKm
) {
}
