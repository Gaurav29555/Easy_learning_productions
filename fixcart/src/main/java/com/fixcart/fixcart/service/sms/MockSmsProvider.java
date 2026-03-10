package com.fixcart.fixcart.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MockSmsProvider implements SmsProvider {

    @Override
    public void sendOtp(String phone, String message) {
        log.info("fixcart mock sms phone={} message={}", phone, message);
    }
}
