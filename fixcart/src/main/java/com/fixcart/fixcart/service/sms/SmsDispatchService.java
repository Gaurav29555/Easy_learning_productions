package com.fixcart.fixcart.service.sms;

import com.fixcart.fixcart.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsDispatchService {

    private final TwilioSmsProvider twilioSmsProvider;
    private final Fast2SmsProvider fast2SmsProvider;
    private final MockSmsProvider mockSmsProvider;

    @Value("${fixcart.sms.provider:AUTO}")
    private String smsProvider;
    @Value("${fixcart.sms.twilio.account-sid:}")
    private String twilioAccountSid;
    @Value("${fixcart.sms.twilio.auth-token:}")
    private String twilioAuthToken;
    @Value("${fixcart.sms.twilio.from-number:}")
    private String twilioFromNumber;
    @Value("${fixcart.sms.fast2sms.api-key:}")
    private String fast2SmsApiKey;

    public void sendOtp(String phone, String message) {
        String mode = smsProvider.toUpperCase();
        if ("AUTO".equals(mode)) {
            mode = detectProviderByConfiguredCredentials();
        }

        switch (mode) {
            case "TWILIO" -> twilioSmsProvider.sendOtp(phone, message);
            case "FAST2SMS" -> fast2SmsProvider.sendOtp(phone, message);
            case "MOCK" -> mockSmsProvider.sendOtp(phone, message);
            default -> throw new BadRequestException("Unsupported SMS provider: " + smsProvider);
        }
    }

    private String detectProviderByConfiguredCredentials() {
        boolean twilioConfigured = !twilioAccountSid.isBlank() && !twilioAuthToken.isBlank() && !twilioFromNumber.isBlank();
        if (twilioConfigured) {
            return "TWILIO";
        }
        boolean fast2SmsConfigured = !fast2SmsApiKey.isBlank();
        if (fast2SmsConfigured) {
            return "FAST2SMS";
        }
        return "MOCK";
    }
}
