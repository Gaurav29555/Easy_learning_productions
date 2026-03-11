package com.fixcart.fixcart.controller;

import com.fixcart.fixcart.dto.RouteSimulationResponse;
import com.fixcart.fixcart.dto.TrackingEventResponse;
import com.fixcart.fixcart.dto.TrackingUpdateRequest;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.service.TrackingService;
import com.fixcart.fixcart.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;
    private final UserService userService;

    @PostMapping("/bookings/{bookingId}/location")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN')")
    public ResponseEntity<TrackingEventResponse> publishLocation(
            Principal principal,
            @PathVariable Long bookingId,
            @Valid @RequestBody TrackingUpdateRequest request
    ) {
        Long userId = userService.extractUserId(principal.getName());
        UserRole role = userService.extractRole(principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trackingService.publishLocation(bookingId, userId, role, request));
    }

    @GetMapping("/bookings/{bookingId}/events")
    @PreAuthorize("hasAnyRole('CUSTOMER','WORKER','ADMIN')")
    public ResponseEntity<List<TrackingEventResponse>> getRecentEvents(
            Principal principal,
            @PathVariable Long bookingId
    ) {
        Long userId = userService.extractUserId(principal.getName());
        UserRole role = userService.extractRole(principal.getName());
        return ResponseEntity.ok(trackingService.getRecentEvents(bookingId, userId, role));
    }

    @GetMapping("/bookings/{bookingId}/route")
    @PreAuthorize("hasAnyRole('CUSTOMER','WORKER','ADMIN')")
    public ResponseEntity<RouteSimulationResponse> getRouteSimulation(
            Principal principal,
            @PathVariable Long bookingId
    ) {
        Long userId = userService.extractUserId(principal.getName());
        UserRole role = userService.extractRole(principal.getName());
        return ResponseEntity.ok(trackingService.simulateRoute(bookingId, userId, role));
    }
}
