package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.PaymentProvider;
import com.fixcart.fixcart.entity.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long bookingId,
        Long customerId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentProvider provider,
        String providerOrderId,
        String providerPaymentId,
        String providerClientSecret,
        String failureReason,
        LocalDateTime createdAt
) {
}
