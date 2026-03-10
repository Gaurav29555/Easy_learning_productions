package com.fixcart.fixcart.controller;

import com.fixcart.fixcart.dto.AdminMetricsResponse;
import com.fixcart.fixcart.dto.AuditLogResponse;
import com.fixcart.fixcart.dto.BookingResponse;
import com.fixcart.fixcart.dto.PaymentResponse;
import com.fixcart.fixcart.dto.UpdateWorkerApprovalStatusRequest;
import com.fixcart.fixcart.dto.UpdateWorkerAvailabilityRequest;
import com.fixcart.fixcart.dto.WorkerResponse;
import com.fixcart.fixcart.service.AdminService;
import com.fixcart.fixcart.service.BookingService;
import com.fixcart.fixcart.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final BookingService bookingService;
    private final PaymentService paymentService;

    @GetMapping("/metrics")
    public ResponseEntity<AdminMetricsResponse> metrics() {
        return ResponseEntity.ok(adminService.getMetrics());
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> recentBookings() {
        return ResponseEntity.ok(adminService.getRecentBookings());
    }

    @GetMapping("/workers")
    public ResponseEntity<List<WorkerResponse>> workers() {
        return ResponseEntity.ok(adminService.getWorkers());
    }

    @PatchMapping("/workers/{workerId}/availability")
    public ResponseEntity<WorkerResponse> workerAvailability(
            @PathVariable Long workerId,
            @Valid @RequestBody UpdateWorkerAvailabilityRequest request
    ) {
        return ResponseEntity.ok(adminService.updateWorkerAvailability(workerId, request));
    }

    @PatchMapping("/workers/{workerId}/approval")
    public ResponseEntity<WorkerResponse> workerApproval(
            @PathVariable Long workerId,
            @Valid @RequestBody UpdateWorkerApprovalStatusRequest request
    ) {
        return ResponseEntity.ok(adminService.updateWorkerApprovalStatus(workerId, request));
    }

    @PostMapping("/bookings/{bookingId}/assign")
    public ResponseEntity<BookingResponse> assignWorker(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.assignNearestWorker(bookingId));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>> payments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> auditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }
}
