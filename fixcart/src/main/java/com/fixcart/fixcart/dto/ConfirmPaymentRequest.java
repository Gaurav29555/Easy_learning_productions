package com.fixcart.fixcart.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
        @NotBlank String providerOrderId,
        @NotBlank String providerPaymentId
) {
}
