package com.fixcart.fixcart.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDispatchService {

    private final JavaMailSender mailSender;

    @Value("${fixcart.email.provider:UI_ONLY}")
    private String emailProvider;

    @Value("${fixcart.email.from-address:}")
    private String fromAddress;

    @Value("${fixcart.email.from-name:fixcart}")
    private String fromName;

    public void sendOtp(String toEmail, String otpCode, int expirationMinutes) {
        if ("UI_ONLY".equalsIgnoreCase(emailProvider) || "MOCK".equalsIgnoreCase(emailProvider)) {
            log.info("fixcart UI-only OTP mode active for email={}", toEmail);
            return;
        }

        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("fixcart email sender not configured. Falling back to UI-only OTP for email={}", toEmail);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Your fixcart OTP");
        message.setText(
                "Hello from " + fromName + ",\n\n"
                        + "Your fixcart OTP is " + otpCode + ".\n"
                        + "It expires in " + expirationMinutes + " minutes.\n\n"
                        + "If you did not request this, ignore this email."
        );
        mailSender.send(message);
    }
}
