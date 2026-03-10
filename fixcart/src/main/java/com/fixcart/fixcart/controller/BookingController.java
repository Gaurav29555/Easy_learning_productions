package com.fixcart.fixcart.controller;

import com.fixcart.fixcart.dto.BookingResponse;
import com.fixcart.fixcart.dto.CreateBookingRequest;
import com.fixcart.fixcart.dto.UpdateBookingStatusRequest;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.service.BookingService;
import com.fixcart.fixcart.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<BookingResponse> createBooking(
            Principal principal,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        Long customerId = userService.extractUserId(principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(customerId, request));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(Principal principal, @PathVariable Long bookingId) {
        Long userId = userService.extractUserId(principal.getName());
        UserRole role = userService.extractRole(principal.getName());
        return ResponseEntity.ok(bookingService.getBooking(bookingId, userId, role));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Principal principal) {
        Long userId = userService.extractUserId(principal.getName());
        return ResponseEntity.ok(bookingService.getCustomerBookings(userId));
    }

    @GetMapping("/worker")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN')")
    public ResponseEntity<List<BookingResponse>> getWorkerBookings(Principal principal) {
        Long userId = userService.extractUserId(principal.getName());
        return ResponseEntity.ok(bookingService.getWorkerBookings(userId));
    }

    @PostMapping("/{bookingId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> assignNearest(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.assignNearestWorker(bookingId));
    }

    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<BookingResponse> updateStatus(
            Principal principal,
            @PathVariable Long bookingId,
            @Valid @RequestBody UpdateBookingStatusRequest request
    ) {
        Long userId = userService.extractUserId(principal.getName());
        UserRole role = userService.extractRole(principal.getName());
        return ResponseEntity.ok(bookingService.updateStatus(bookingId, request.status(), request.cancellationReason(), userId, role));
    }
}
