package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.AdminMetricsResponse;
import com.fixcart.fixcart.dto.AuditLogResponse;
import com.fixcart.fixcart.dto.BookingResponse;
import com.fixcart.fixcart.dto.UpdateWorkerApprovalStatusRequest;
import com.fixcart.fixcart.dto.UpdateWorkerAvailabilityRequest;
import com.fixcart.fixcart.dto.WorkerResponse;
import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.AuditActionType;
import com.fixcart.fixcart.entity.enums.BookingStatus;
import com.fixcart.fixcart.entity.enums.PaymentStatus;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.entity.enums.WorkerApprovalStatus;
import com.fixcart.fixcart.exception.ResourceNotFoundException;
import com.fixcart.fixcart.repository.BookingRepository;
import com.fixcart.fixcart.repository.PaymentRepository;
import com.fixcart.fixcart.repository.UserRepository;
import com.fixcart.fixcart.repository.WorkerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final WorkerService workerService;
    private final BookingService bookingService;
    private final AuditLogService auditLogService;

    public AdminMetricsResponse getMetrics() {
        return new AdminMetricsResponse(
                userRepository.countByRole(UserRole.CUSTOMER),
                userRepository.countByRole(UserRole.WORKER),
                workerRepository.countByAvailableTrue(),
                bookingRepository.count(),
                bookingRepository.countByStatus(BookingStatus.COMPLETED),
                bookingRepository.countByStatus(BookingStatus.CANCELLED),
                paymentRepository.countByStatus(PaymentStatus.SUCCESS)
        );
    }

    public List<BookingResponse> getRecentBookings() {
        return bookingRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(bookingService::toResponse)
                .toList();
    }

    public List<WorkerResponse> getWorkers() {
        return workerRepository.findAll().stream()
                .map(worker -> workerService.mapWorkerWithDistance(worker, worker.getLatitude(), worker.getLongitude()))
                .toList();
    }

    @Transactional
    public WorkerResponse updateWorkerAvailability(Long workerId, UpdateWorkerAvailabilityRequest request) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
        worker.setAvailable(request.available());
        Worker saved = workerRepository.save(worker);
        return workerService.mapWorkerWithDistance(saved, saved.getLatitude(), saved.getLongitude());
    }

    @Transactional
    public WorkerResponse updateWorkerApprovalStatus(Long workerId, UpdateWorkerApprovalStatusRequest request) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
        worker.setApprovalStatus(request.approvalStatus());
        if (request.approvalStatus() != WorkerApprovalStatus.APPROVED) {
            worker.setAvailable(false);
        }
        Worker saved = workerRepository.save(worker);
        auditLogService.record(
                AuditActionType.WORKER_APPROVAL_UPDATED,
                "ADMIN",
                null,
                "WORKER",
                saved.getId(),
                "Worker approval changed to " + request.approvalStatus()
        );
        return workerService.mapWorkerWithDistance(saved, saved.getLatitude(), saved.getLongitude());
    }

    public List<AuditLogResponse> getAuditLogs() {
        return auditLogService.recentLogs();
    }
}
