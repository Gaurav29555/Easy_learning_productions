package com.fixcart.fixcart.dto;

import java.util.List;

public record VoiceCommandResponse(
        String action,
        String spokenResponse,
        BookingResponse booking,
        RouteSimulationResponse route,
        List<WorkerResponse> workers,
        List<AddressSuggestionResponse> addressSuggestions,
        List<String> suggestions
) {
}
