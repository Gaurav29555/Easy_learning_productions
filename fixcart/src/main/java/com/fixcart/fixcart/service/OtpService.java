package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.OtpStatusResponse;
import com.fixcart.fixcart.entity.OtpVerification;
import com.fixcart.fixcart.entity.enums.AuditActionType;
import com.fixcart.fixcart.entity.enums.OtpPurpose;
import com.fixcart.fixcart.exception.BadRequestException;
import com.fixcart.fixcart.repository.OtpVerificationRepository;
import com.fixcart.fixcart.service.email.EmailDispatchService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailDispatchService emailDispatchService;
    private final RequestRateLimitService requestRateLimitService;
    private final AuditLogService auditLogService;
    private final Random random = new Random();

    @Value("${fixcart.otp.expiration-minutes:5}")
    private int otpExpirationMinutes;

    @Value("${fixcart.otp.max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${fixcart.otp.send-max-requests:25}")
    private int otpSendMaxRequests;

    @Value("${fixcart.otp.verify-max-requests:50}")
    private int otpVerifyMaxRequests;

    @Value("${fixcart.otp.rate-limit-window-minutes:15}")
    private int otpRateLimitWindowMinutes;

    @Value("${fixcart.otp.expose-in-response:true}")
    private boolean exposeOtpInResponse;

    @Transactional
    public OtpStatusResponse sendOtp(String email, OtpPurpose purpose) {
        String normalizedEmail = email.toLowerCase().trim();
        requestRateLimitService.assertAllowed(
                "otp:send:" + normalizedEmail,
                otpSendMaxRequests,
                Duration.ofMinutes(otpRateLimitWindowMinutes)
        );
        String otpCode = String.format("%06d", random.nextInt(1_000_000));
        OtpVerification otp = new OtpVerification();
        otp.setEmail(normalizedEmail);
        otp.setOtpCode(otpCode);
        otp.setPurpose(purpose);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        otp.setVerified(false);
        otp.setConsumed(false);
        otp.setAttempts(0);
        otpVerificationRepository.save(otp);
        emailDispatchService.sendOtp(normalizedEmail, otpCode, otpExpirationMinutes);
        auditLogService.record(AuditActionType.OTP_SENT, "SYSTEM", null, "OTP", otp.getId(), "OTP sent for email " + normalizedEmail + " purpose " + purpose);

        log.info("fixcart OTP generated email={} purpose={} otp={}", normalizedEmail, purpose, otpCode);
        return new OtpStatusResponse(
                true,
                "OTP sent successfully to email",
                exposeOtpInResponse ? otpCode : null
        );
    }

    @Transactional
    public OtpStatusResponse verifyOtp(String email, OtpPurpose purpose, String otpCode) {
        String normalizedEmail = email.toLowerCase().trim();
        requestRateLimitService.assertAllowed(
                "otp:verify:" + normalizedEmail,
                otpVerifyMaxRequests,
                Duration.ofMinutes(otpRateLimitWindowMinutes)
        );
        OtpVerification otp = otpVerificationRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(normalizedEmail, purpose)
                .orElseThrow(() -> new BadRequestException("No OTP found. Please request a new OTP."));

        if (otp.isConsumed()) {
            throw new BadRequestException("OTP already consumed");
        }
        if (otp.isVerified()) {
            return new OtpStatusResponse(true, "OTP already verified", null);
        }
        if (otp.getAttempts() >= otpMaxAttempts) {
            throw new BadRequestException("OTP attempts exceeded. Request a new OTP.");
        }
        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            throw new BadRequestException("OTP expired");
        }
        if (!otp.getOtpCode().equals(otpCode)) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpVerificationRepository.save(otp);
            throw new BadRequestException("Invalid OTP");
        }

        otp.setVerified(true);
        otpVerificationRepository.save(otp);
        auditLogService.record(AuditActionType.OTP_VERIFIED, "SYSTEM", null, "OTP", otp.getId(), "OTP verified for email " + normalizedEmail + " purpose " + purpose);
        return new OtpStatusResponse(true, "OTP verified successfully", null);
    }

    public void assertEmailVerifiedForPurpose(String email, OtpPurpose purpose) {
        OtpVerification otp = otpVerificationRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email.toLowerCase().trim(), purpose)
                .orElseThrow(() -> new BadRequestException("Email verification required"));
        if (!otp.isVerified() || otp.isConsumed() || LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            throw new BadRequestException("Email verification required");
        }
    }

    @Transactional
    public void consumeVerifiedOtp(String email, OtpPurpose purpose) {
        OtpVerification otp = otpVerificationRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email.toLowerCase().trim(), purpose)
                .orElseThrow(() -> new BadRequestException("Email verification record not found"));
        if (!otp.isVerified()) {
            throw new BadRequestException("Email verification required");
        }
        otp.setConsumed(true);
        otpVerificationRepository.save(otp);
    }
}
