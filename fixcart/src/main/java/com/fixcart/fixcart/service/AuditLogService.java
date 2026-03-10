package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.AuditLogResponse;
import com.fixcart.fixcart.entity.AuditLog;
import com.fixcart.fixcart.entity.enums.AuditActionType;
import com.fixcart.fixcart.repository.AuditLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void record(AuditActionType actionType, String actorType, Long actorId, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setActionType(actionType);
        log.setActorType(actorType);
        log.setActorId(actorId);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> recentLogs() {
        return auditLogRepository.findTop200ByOrderByCreatedAtDesc().stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getActionType(),
                        log.getActorType(),
                        log.getActorId(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}
