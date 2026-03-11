package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.AdminMetricsResponse;
import com.fixcart.fixcart.dto.AuditLogResponse;
import com.fixcart.fixcart.dto.BookingResponse;
import com.fixcart.fixcart.dto.ServiceDemandMetric;
import com.fixcart.fixcart.dto.UpdateWorkerApprovalStatusRequest;
import com.fixcart.fixcart.dto.UpdateWorkerAvailabilityRequest;
import com.fixcart.fixcart.dto.WorkerResponse;
import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.AuditActionType;
import com.fixcart.fixcart.entity.enums.BookingStatus;
import com.fixcart.fixcart.entity.enums.PaymentStatus;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.entity.enums.WorkerApprovalStatus;
import com.fixcart.fixcart.entity.enums.WorkerType;
import com.fixcart.fixcart.exception.ResourceNotFoundException;
import com.fixcart.fixcart.repository.BookingRepository;
import com.fixcart.fixcart.repository.PaymentRepository;
import com.fixcart.fixcart.repository.UserRepository;
import com.fixcart.fixcart.repository.WorkerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
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
        long totalCustomers = userRepository.countByRole(UserRole.CUSTOMER);
        long totalWorkers = userRepository.countByRole(UserRole.WORKER);
        long availableWorkers = workerRepository.countByAvailableTrue();
        long totalBookings = bookingRepository.count();
        long completedBookings = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long successfulPayments = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        long activeBookings = bookingRepository.countByStatusIn(List.of(BookingStatus.ASSIGNED, BookingStatus.IN_PROGRESS));
        long pendingWorkerApprovals = workerRepository.countByApprovalStatus(WorkerApprovalStatus.PENDING_REVIEW);
        long assignedBookings = bookingRepository.countByWorkerIsNotNull();
        BigDecimal gmv = paymentRepository.sumSuccessfulPayments();
        BigDecimal averageOrderValue = successfulPayments == 0
                ? BigDecimal.ZERO
                : gmv.divide(BigDecimal.valueOf(successfulPayments), 2, RoundingMode.HALF_UP);
        double completionRate = totalBookings == 0 ? 0 : (completedBookings * 100.0) / totalBookings;
        double cancellationRate = totalBookings == 0 ? 0 : (cancelledBookings * 100.0) / totalBookings;
        double workerUtilizationRate = totalWorkers == 0 ? 0 : (activeBookings * 100.0) / totalWorkers;
        double assignmentRate = totalBookings == 0 ? 0 : (assignedBookings * 100.0) / totalBookings;
        List<ServiceDemandMetric> serviceDemand = Arrays.stream(WorkerType.values())
                .map(type -> new ServiceDemandMetric(type.name(), bookingRepository.countByServiceType(type)))
                .sorted((left, right) -> Long.compare(right.totalBookings(), left.totalBookings()))
                .toList();

        return new AdminMetricsResponse(
                totalCustomers,
                totalWorkers,
                availableWorkers,
                totalBookings,
                completedBookings,
                cancelledBookings,
                successfulPayments,
                activeBookings,
                pendingWorkerApprovals,
                gmv,
                averageOrderValue,
                completionRate,
                cancellationRate,
                workerUtilizationRate,
                assignmentRate,
                serviceDemand
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
