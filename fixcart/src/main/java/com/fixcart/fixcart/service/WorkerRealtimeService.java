package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.WorkerRealtimeEvent;
import com.fixcart.fixcart.dto.WorkerResponse;
import com.fixcart.fixcart.entity.Worker;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkerRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishLocationUpdate(Worker worker) {
        WorkerResponse response = new WorkerResponse(
                worker.getId(),
                worker.getUser().getId(),
                worker.getUser().getFullName(),
                worker.getWorkerType(),
                worker.getApprovalStatus(),
                worker.getKycDocumentUrl(),
                worker.getYearsOfExperience(),
                worker.getLatitude(),
                worker.getLongitude(),
                worker.isAvailable(),
                0
        );
        WorkerRealtimeEvent event = new WorkerRealtimeEvent("WORKER_LOCATION_UPDATED", worker.getWorkerType(), response);
        messagingTemplate.convertAndSend("/topic/workers/all", event);
        messagingTemplate.convertAndSend("/topic/workers/" + worker.getWorkerType().name(), event);
    }
}
