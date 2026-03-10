package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.AuditActionType;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        AuditActionType actionType,
        String actorType,
        Long actorId,
        String entityType,
        Long entityId,
        String details,
        LocalDateTime createdAt
) {
}
