package com.fixcart.fixcart.service.sms;

import com.fixcart.fixcart.exception.BadRequestException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwilioSmsProvider implements SmsProvider {

    @Value("${fixcart.sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${fixcart.sms.twilio.auth-token:}")
    private String authToken;

    @Value("${fixcart.sms.twilio.from-number:}")
    private String fromNumber;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void sendOtp(String phone, String message) {
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            throw new BadRequestException("Twilio SMS provider is not configured");
        }
        try {
            String body = "To=" + URLEncoder.encode(phone, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(fromNumber, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            String credentials = Base64.getEncoder()
                    .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Twilio SMS failed: " + response.body());
            }
        } catch (Exception exception) {
            throw new BadRequestException("Twilio SMS dispatch failed: " + exception.getMessage());
        }
    }
}
