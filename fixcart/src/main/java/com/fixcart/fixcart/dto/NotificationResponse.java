package com.fixcart.fixcart.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
}
