package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.PaymentProvider;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePaymentOrderRequest(
        @NotNull Long bookingId,
        @NotNull @DecimalMin("1.00") BigDecimal amount,
        @NotBlank String currency,
        @NotNull PaymentProvider provider
) {
}
