package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.OtpStatusResponse;
import com.fixcart.fixcart.entity.OtpVerification;
import com.fixcart.fixcart.entity.enums.OtpPurpose;
import com.fixcart.fixcart.exception.BadRequestException;
import com.fixcart.fixcart.repository.OtpVerificationRepository;
import com.fixcart.fixcart.service.sms.SmsDispatchService;
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
    private final SmsDispatchService smsDispatchService;
    private final Random random = new Random();

    @Value("${fixcart.otp.expiration-minutes:5}")
    private int otpExpirationMinutes;

    @Value("${fixcart.otp.max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${fixcart.otp.expose-in-response:true}")
    private boolean exposeOtpInResponse;

    @Transactional
    public OtpStatusResponse sendOtp(String phone, OtpPurpose purpose) {
        String otpCode = String.format("%06d", random.nextInt(1_000_000));
        OtpVerification otp = new OtpVerification();
        otp.setPhone(phone);
        otp.setOtpCode(otpCode);
        otp.setPurpose(purpose);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        otp.setVerified(false);
        otp.setConsumed(false);
        otp.setAttempts(0);
        otpVerificationRepository.save(otp);
        smsDispatchService.sendOtp(phone, "Your fixcart OTP is " + otpCode + ". It expires in " + otpExpirationMinutes + " minutes.");

        log.info("fixcart OTP generated phone={} purpose={} otp={}", phone, purpose, otpCode);
        return new OtpStatusResponse(
                true,
                "OTP generated successfully",
                exposeOtpInResponse ? otpCode : null
        );
    }

    @Transactional
    public OtpStatusResponse verifyOtp(String phone, OtpPurpose purpose, String otpCode) {
        OtpVerification otp = otpVerificationRepository.findFirstByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
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
        return new OtpStatusResponse(true, "OTP verified successfully", null);
    }

    public void assertPhoneVerifiedForPurpose(String phone, OtpPurpose purpose) {
        OtpVerification otp = otpVerificationRepository.findFirstByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
                .orElseThrow(() -> new BadRequestException("Phone verification required"));
        if (!otp.isVerified() || otp.isConsumed() || LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            throw new BadRequestException("Phone verification required");
        }
    }

    @Transactional
    public void consumeVerifiedOtp(String phone, OtpPurpose purpose) {
        OtpVerification otp = otpVerificationRepository.findFirstByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
                .orElseThrow(() -> new BadRequestException("Phone verification record not found"));
        if (!otp.isVerified()) {
            throw new BadRequestException("Phone verification required");
        }
        otp.setConsumed(true);
        otpVerificationRepository.save(otp);
    }
}
