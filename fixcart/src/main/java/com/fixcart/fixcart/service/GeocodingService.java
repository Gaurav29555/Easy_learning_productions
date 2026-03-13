package com.fixcart.fixcart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixcart.fixcart.dto.AddressSuggestionResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final AddressSearchService addressSearchService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Value("${fixcart.geocoding.provider:AUTO}")
    private String provider;

    @Value("${fixcart.geocoding.nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String nominatimBaseUrl;

    @Value("${fixcart.geocoding.user-agent:fixcart/1.0}")
    private String userAgent;

    public List<AddressSuggestionResponse> search(String query, Double nearLatitude, Double nearLongitude) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        if ("SAMPLE".equalsIgnoreCase(provider)) {
            return addressSearchService.search(query, nearLatitude, nearLongitude);
        }

        try {
            List<AddressSuggestionResponse> external = searchExternal(query, nearLatitude, nearLongitude);
            if (!external.isEmpty()) {
                return external;
            }
        } catch (Exception ignored) {
            // Fallback intentionally keeps fixcart usable without a third-party dependency.
        }
        return addressSearchService.search(query, nearLatitude, nearLongitude);
    }

    public AddressSuggestionResponse resolveBestMatch(String query, Double nearLatitude, Double nearLongitude) {
        List<AddressSuggestionResponse> results = search(query, nearLatitude, nearLongitude);
        return results.isEmpty() ? null : results.get(0);
    }

    private List<AddressSuggestionResponse> searchExternal(String query, Double nearLatitude, Double nearLongitude) throws Exception {
        StringBuilder url = new StringBuilder(nominatimBaseUrl)
                .append("/search?format=jsonv2&limit=6&q=")
                .append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        if (nearLatitude != null && nearLongitude != null) {
            url.append("&viewbox=")
                    .append(nearLongitude - 0.25).append(",")
                    .append(nearLatitude + 0.25).append(",")
                    .append(nearLongitude + 0.25).append(",")
                    .append(nearLatitude - 0.25);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            return List.of();
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (!root.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(root.spliterator(), false)
                .map(node -> new AddressSuggestionResponse(
                        node.path("display_name").asText("Unknown address"),
                        node.path("lat").asDouble(),
                        node.path("lon").asDouble(),
                        node.path("address").path("country_code").asText("").toUpperCase(),
                        "NOMINATIM"
                ))
                .filter(item -> !(item.latitude() == 0 && item.longitude() == 0))
                .toList();
    }
}
