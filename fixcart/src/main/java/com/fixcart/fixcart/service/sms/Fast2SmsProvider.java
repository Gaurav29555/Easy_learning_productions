package com.fixcart.fixcart.service.sms;

import com.fixcart.fixcart.exception.BadRequestException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Fast2SmsProvider implements SmsProvider {

    @Value("${fixcart.sms.fast2sms.api-key:}")
    private String apiKey;

    @Value("${fixcart.sms.fast2sms.route:dlt}")
    private String route;

    @Value("${fixcart.sms.fast2sms.sender-id:FIXCRT}")
    private String senderId;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void sendOtp(String phone, String message) {
        if (apiKey.isBlank()) {
            throw new BadRequestException("Fast2SMS provider is not configured");
        }
        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String url = "https://www.fast2sms.com/dev/bulkV2"
                    + "?route=" + URLEncoder.encode(route, StandardCharsets.UTF_8)
                    + "&sender_id=" + URLEncoder.encode(senderId, StandardCharsets.UTF_8)
                    + "&message=" + encodedMessage
                    + "&language=english"
                    + "&numbers=" + URLEncoder.encode(phone, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("authorization", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Fast2SMS failed: " + response.body());
            }
        } catch (Exception exception) {
            throw new BadRequestException("Fast2SMS dispatch failed: " + exception.getMessage());
        }
    }
}
