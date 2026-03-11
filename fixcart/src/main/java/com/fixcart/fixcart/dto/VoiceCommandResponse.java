package com.fixcart.fixcart.dto;

import java.util.List;

public record VoiceCommandResponse(
        String action,
        String spokenResponse,
        BookingResponse booking,
        List<WorkerResponse> workers,
        List<String> suggestions
) {
}
