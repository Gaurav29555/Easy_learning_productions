package com.fixcart.fixcart.dto;

public record WebhookAckResponse(
        boolean success,
        String message
) {
}
