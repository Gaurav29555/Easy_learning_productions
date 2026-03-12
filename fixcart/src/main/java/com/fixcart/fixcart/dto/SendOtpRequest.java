package com.fixcart.fixcart.dto;

import com.fixcart.fixcart.entity.enums.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendOtpRequest(
        @NotBlank @Email String email,
        @NotNull OtpPurpose purpose
) {
}
