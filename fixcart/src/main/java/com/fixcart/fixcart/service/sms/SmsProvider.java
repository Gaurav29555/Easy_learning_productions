package com.fixcart.fixcart.service.sms;

public interface SmsProvider {

    void sendOtp(String phone, String message);
}
