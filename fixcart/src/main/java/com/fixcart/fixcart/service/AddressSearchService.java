package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.AddressSuggestionResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AddressSearchService {

    private static final List<SampleAddress> SAMPLE_ADDRESSES = List.of(
            new SampleAddress("Baner, Pune, India", 18.5590, 73.7868, "IN"),
            new SampleAddress("Shivajinagar, Pune, India", 18.5308, 73.8475, "IN"),
            new SampleAddress("Andheri West, Mumbai, India", 19.1364, 72.8276, "IN"),
            new SampleAddress("Powai, Mumbai, India", 19.1197, 72.9050, "IN"),
            new SampleAddress("HSR Layout, Bengaluru, India", 12.9116, 77.6474, "IN"),
            new SampleAddress("Connaught Place, Delhi, India", 28.6315, 77.2167, "IN"),
            new SampleAddress("Times Square, New York, USA", 40.7580, -73.9855, "US"),
            new SampleAddress("Downtown San Francisco, USA", 37.7749, -122.4194, "US"),
            new SampleAddress("Central Madrid, Spain", 40.4168, -3.7038, "ES"),
            new SampleAddress("Barcelona City Center, Spain", 41.3874, 2.1686, "ES")
    );

    public List<AddressSuggestionResponse> search(String query, Double nearLatitude, Double nearLongitude) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        return SAMPLE_ADDRESSES.stream()
                .filter(address -> normalize(address.label()).contains(normalizedQuery))
                .map(address -> new RankedAddress(address, scoreAddress(address, normalizedQuery, nearLatitude, nearLongitude)))
                .sorted(Comparator.comparingDouble(RankedAddress::score).reversed())
                .limit(6)
                .map(ranked -> new AddressSuggestionResponse(
                        ranked.address().label(),
                        ranked.address().latitude(),
                        ranked.address().longitude(),
                        ranked.address().regionCode(),
                        "FIXCART_SAMPLE"
                ))
                .toList();
    }

    public AddressSuggestionResponse resolveBestMatch(String query, Double nearLatitude, Double nearLongitude) {
        List<AddressSuggestionResponse> results = search(query, nearLatitude, nearLongitude);
        if (!results.isEmpty()) {
            return results.get(0);
        }

        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return null;
        }

        List<String> tokens = new ArrayList<>(List.of(normalizedQuery.split(" ")));
        return SAMPLE_ADDRESSES.stream()
                .map(address -> new RankedAddress(
                        address,
                        tokens.stream().filter(token -> !token.isBlank() && normalize(address.label()).contains(token)).count()
                ))
                .filter(ranked -> ranked.score() > 0)
                .sorted(Comparator.comparingDouble(RankedAddress::score).reversed())
                .map(ranked -> new AddressSuggestionResponse(
                        ranked.address().label(),
                        ranked.address().latitude(),
                        ranked.address().longitude(),
                        ranked.address().regionCode(),
                        "FIXCART_SAMPLE"
                ))
                .findFirst()
                .orElse(null);
    }

    private double scoreAddress(SampleAddress address, String normalizedQuery, Double nearLatitude, Double nearLongitude) {
        double score = normalize(address.label()).equals(normalizedQuery) ? 10 : 5;
        if (nearLatitude != null && nearLongitude != null) {
            double distance = haversineKm(nearLatitude, nearLongitude, address.latitude(), address.longitude());
            score += Math.max(0, 5 - distance / 25);
        }
        return score;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT).replace(",", " ");
    }

    private record SampleAddress(String label, double latitude, double longitude, String regionCode) {
    }

    private record RankedAddress(SampleAddress address, double score) {
    }
}
