package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.RouteEtaResponse;
import com.fixcart.fixcart.dto.RoutePointResponse;
import com.fixcart.fixcart.dto.RouteSimulationResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoutePlanningService {

    private final WorkerService workerService;

    @Value("${fixcart.route.road-factor:1.18}")
    private double roadFactor;

    public RouteSimulationResponse simulateBookingRoute(
            Long bookingId,
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude,
            double speedKmh
    ) {
        double totalDistanceKm = adjustedDistanceKm(originLatitude, originLongitude, destinationLatitude, destinationLongitude);
        long etaMinutes = estimateEtaMinutes(totalDistanceKm, speedKmh);
        List<RoutePointResponse> points = interpolateRoute(originLatitude, originLongitude, destinationLatitude, destinationLongitude);
        return new RouteSimulationResponse(
                bookingId,
                originLatitude,
                originLongitude,
                destinationLatitude,
                destinationLongitude,
                totalDistanceKm,
                etaMinutes,
                points
        );
    }

    public RouteEtaResponse computeEta(
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude,
            double speedKmh
    ) {
        double totalDistanceKm = adjustedDistanceKm(originLatitude, originLongitude, destinationLatitude, destinationLongitude);
        long etaMinutes = estimateEtaMinutes(totalDistanceKm, speedKmh);
        return new RouteEtaResponse(
                originLatitude,
                originLongitude,
                destinationLatitude,
                destinationLongitude,
                totalDistanceKm,
                etaMinutes,
                interpolateRoute(originLatitude, originLongitude, destinationLatitude, destinationLongitude)
        );
    }

    public long estimateEtaMinutes(double distanceKm, double speedKmh) {
        double effectiveSpeed = speedKmh > 5 ? speedKmh : 22;
        return Math.max(1L, Math.round((distanceKm / effectiveSpeed) * 60));
    }

    public double adjustedDistanceKm(double originLatitude, double originLongitude, double destinationLatitude, double destinationLongitude) {
        return workerService.haversineKm(originLatitude, originLongitude, destinationLatitude, destinationLongitude) * roadFactor;
    }

    public List<RoutePointResponse> interpolateRoute(double startLat, double startLng, double endLat, double endLng) {
        List<RoutePointResponse> points = new ArrayList<>();
        int segments = 14;
        for (int i = 0; i <= segments; i++) {
            double ratio = i / (double) segments;
            double bend = Math.sin(Math.PI * ratio) * 0.0025;
            points.add(new RoutePointResponse(
                    startLat + ((endLat - startLat) * ratio) + bend,
                    startLng + ((endLng - startLng) * ratio) - bend
            ));
        }
        return points;
    }
}
