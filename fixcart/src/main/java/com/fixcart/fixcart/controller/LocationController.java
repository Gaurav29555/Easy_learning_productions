package com.fixcart.fixcart.controller;

import com.fixcart.fixcart.dto.AddressSuggestionResponse;
import com.fixcart.fixcart.dto.RouteEtaResponse;
import com.fixcart.fixcart.service.AddressSearchService;
import com.fixcart.fixcart.service.RoutePlanningService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final AddressSearchService addressSearchService;
    private final RoutePlanningService routePlanningService;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CUSTOMER','WORKER','ADMIN')")
    public ResponseEntity<List<AddressSuggestionResponse>> search(
            @RequestParam String query,
            @RequestParam(required = false) Double nearLatitude,
            @RequestParam(required = false) Double nearLongitude
    ) {
        return ResponseEntity.ok(addressSearchService.search(query, nearLatitude, nearLongitude));
    }

    @GetMapping("/route-eta")
    @PreAuthorize("hasAnyRole('CUSTOMER','WORKER','ADMIN')")
    public ResponseEntity<RouteEtaResponse> routeEta(
            @RequestParam double originLatitude,
            @RequestParam double originLongitude,
            @RequestParam double destinationLatitude,
            @RequestParam double destinationLongitude,
            @RequestParam(defaultValue = "22") double speedKmh
    ) {
        return ResponseEntity.ok(routePlanningService.computeEta(
                originLatitude,
                originLongitude,
                destinationLatitude,
                destinationLongitude,
                speedKmh
        ));
    }
}
