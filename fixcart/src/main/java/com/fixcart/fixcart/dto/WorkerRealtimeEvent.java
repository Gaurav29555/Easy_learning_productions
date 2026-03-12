package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.WorkerType;

public record WorkerRealtimeEvent(
        String eventType,
        WorkerType workerType,
        WorkerResponse worker
) {
}
