package com.fixcart.fixcart.service.email;

import com.fixcart.fixcart.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailDispatchService {

    private final JavaMailSender mailSender;

    @Value("${fixcart.email.from-address:}")
    private String fromAddress;

    @Value("${fixcart.email.from-name:fixcart}")
    private String fromName;

    public void sendOtp(String toEmail, String otpCode, int expirationMinutes) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new BadRequestException("Email sender is not configured. Set FIXCART_EMAIL_FROM_ADDRESS.");
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
