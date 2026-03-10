package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{10,15}$") String phone,
        @NotNull OtpPurpose purpose,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otpCode
) {
}
