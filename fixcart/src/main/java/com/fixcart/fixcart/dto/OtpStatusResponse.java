package com.fixcart.fixcart.dto;

public record OtpStatusResponse(
        boolean success,
        String message,
        String debugOtp
) {
}
