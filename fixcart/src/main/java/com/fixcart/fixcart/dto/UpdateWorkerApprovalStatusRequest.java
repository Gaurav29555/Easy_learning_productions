package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.WorkerApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkerApprovalStatusRequest(
        @NotNull WorkerApprovalStatus approvalStatus
) {
}
