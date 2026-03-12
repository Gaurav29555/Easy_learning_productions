package com.fixcart.fixcart.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OtpLoginRequest(
        @NotBlank @Email String email,
        @NotBlank @jakarta.validation.constraints.Pattern(regexp = "^[0-9]{6}$") String otpCode
) {
}
