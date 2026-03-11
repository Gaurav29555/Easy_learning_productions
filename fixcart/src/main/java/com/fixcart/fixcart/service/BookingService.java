package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.BookingResponse;
import com.fixcart.fixcart.dto.CreateBookingRequest;
import com.fixcart.fixcart.entity.Booking;
import com.fixcart.fixcart.entity.User;
import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.AuditActionType;
import com.fixcart.fixcart.entity.enums.BookingStatus;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.exception.BadRequestException;
import com.fixcart.fixcart.exception.ResourceNotFoundException;
import com.fixcart.fixcart.repository.BookingRepository;
import com.fixcart.fixcart.repository.UserRepository;
import com.fixcart.fixcart.repository.WorkerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final WorkerService workerService;
    private final AuditLogService auditLogService;
    private final BookingRealtimeService bookingRealtimeService;

    @Value("${fixcart.booking.assignment-radius-km}")
    private double assignmentRadiusKm;

    @Value("${fixcart.booking.base-price:249}")
    private BigDecimal basePrice;

    @Value("${fixcart.booking.distance-price-per-km:12}")
    private BigDecimal distancePricePerKm;

    @Transactional
    public BookingResponse createBooking(Long customerId, CreateBookingRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setServiceType(request.serviceType());
        booking.setServiceAddress(request.serviceAddress());
        booking.setCustomerLatitude(request.customerLatitude());
        booking.setCustomerLongitude(request.customerLongitude());
        booking.setScheduledAt(request.scheduledAt());
        booking.setNotes(request.notes());
        booking.setEstimatedPrice(calculateEstimatedPrice(request.customerLatitude(), request.customerLongitude(), request.serviceType()));
        booking.setStatus(BookingStatus.PENDING);

        WorkerService.WorkerDistance nearest = findBestWorkerWithRadiusExpansion(
                request.customerLatitude(),
                request.customerLongitude(),
                request.serviceType()
        );

        if (nearest != null && request.scheduledAt() == null) {
            Worker worker = nearest.getWorker();
            worker.setAvailable(false);
            workerRepository.save(worker);
            booking.setWorker(worker);
            booking.setStatus(BookingStatus.ASSIGNED);
        }
        Booking saved = bookingRepository.save(booking);
        auditLogService.record(
                booking.getWorker() == null ? AuditActionType.BOOKING_CREATED : AuditActionType.BOOKING_ASSIGNED,
                "USER",
                customerId,
                "BOOKING",
                saved.getId(),
                booking.getWorker() == null ? "Booking created in fixcart" : "Booking auto-assigned in fixcart"
        );
        bookingRealtimeService.publish(
                booking.getWorker() == null ? "BOOKING_CREATED" : "BOOKING_ASSIGNED",
                booking.getWorker() == null ? "Fixcart created a new booking request." : "Fixcart assigned a worker to the booking.",
                saved
        );
        return toResponse(saved);
    }

    public BookingResponse getBooking(Long bookingId, Long userId, UserRole role) {
        Booking booking = findBookingOrThrow(bookingId);
        validateBookingAccess(booking, userId, role);
        return toResponse(booking);
    }

    public List<BookingResponse> getCustomerBookings(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BookingResponse> getWorkerBookings(Long userId) {
        Worker worker = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));

        return bookingRepository.findByWorkerIdOrderByCreatedAtDesc(worker.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse assignNearestWorker(Long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot reassign completed or cancelled booking");
        }

        WorkerService.WorkerDistance nearest = findBestWorkerWithRadiusExpansion(
                booking.getCustomerLatitude(),
                booking.getCustomerLongitude(),
                booking.getServiceType()
        );

        if (nearest == null) {
            booking.setWorker(null);
            booking.setStatus(BookingStatus.PENDING);
            Booking saved = bookingRepository.save(booking);
            bookingRealtimeService.publish("BOOKING_PENDING", "Fixcart could not find an available worker yet.", saved);
            return toResponse(saved);
        }

        Worker worker = nearest.getWorker();
        worker.setAvailable(false);
        workerRepository.save(worker);

        booking.setWorker(worker);
        booking.setStatus(BookingStatus.ASSIGNED);
        Booking saved = bookingRepository.save(booking);
        auditLogService.record(AuditActionType.BOOKING_ASSIGNED, "ADMIN", null, "BOOKING", saved.getId(), "Booking assigned to nearest approved worker");
        bookingRealtimeService.publish("BOOKING_ASSIGNED", "Fixcart assigned a worker to the booking.", saved);
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse updateStatus(Long bookingId, BookingStatus status, String cancellationReason, Long userId, UserRole role) {
        Booking booking = findBookingOrThrow(bookingId);
        validateBookingAccess(booking, userId, role);

        if (status == BookingStatus.ASSIGNED || status == BookingStatus.PENDING) {
            throw new BadRequestException("Use assignment flow for PENDING or ASSIGNED status");
        }

        if (status == BookingStatus.CANCELLED && role == UserRole.WORKER) {
            throw new BadRequestException("Worker cannot cancel booking");
        }

        if (status == BookingStatus.CANCELLED && (cancellationReason == null || cancellationReason.isBlank())) {
            throw new BadRequestException("Cancellation reason is required");
        }

        booking.setStatus(status);
        booking.setCancellationReason(status == BookingStatus.CANCELLED ? cancellationReason : null);
        Booking saved = bookingRepository.save(booking);

        if (status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED) {
            Worker worker = saved.getWorker();
            if (worker != null) {
                worker.setAvailable(true);
                workerRepository.save(worker);
            }
        }

        auditLogService.record(AuditActionType.BOOKING_STATUS_UPDATED, role.name(), userId, "BOOKING", saved.getId(), "Booking status changed to " + status);
        bookingRealtimeService.publish("BOOKING_STATUS_UPDATED", "Booking status changed to " + status + " in fixcart.", saved);
        return toResponse(saved);
    }

    public Booking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    public void validateBookingAccess(Booking booking, Long userId, UserRole role) {
        if (role == UserRole.ADMIN) {
            return;
        }
        boolean isCustomer = booking.getCustomer().getId().equals(userId);
        boolean isAssignedWorker = booking.getWorker() != null && booking.getWorker().getUser().getId().equals(userId);
        if (!isCustomer && !isAssignedWorker) {
            throw new BadRequestException("User cannot access this booking");
        }
    }

    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCustomer().getId(),
                booking.getWorker() == null ? null : booking.getWorker().getId(),
                booking.getServiceType(),
                booking.getServiceAddress(),
                booking.getCustomerLatitude(),
                booking.getCustomerLongitude(),
                booking.getScheduledAt(),
                booking.getNotes(),
                booking.getEstimatedPrice(),
                booking.getCancellationReason(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    private BigDecimal calculateEstimatedPrice(double customerLatitude, double customerLongitude, com.fixcart.fixcart.entity.enums.WorkerType workerType) {
        WorkerService.WorkerDistance nearest = workerService.findNearestWorker(customerLatitude, customerLongitude, workerType, assignmentRadiusKm * 2);
        BigDecimal distanceCharge = nearest == null
                ? BigDecimal.ZERO
                : distancePricePerKm.multiply(BigDecimal.valueOf(nearest.getDistanceKm()));
        return basePrice.add(distanceCharge).setScale(2, RoundingMode.HALF_UP);
    }

    private WorkerService.WorkerDistance findBestWorkerWithRadiusExpansion(
            double latitude,
            double longitude,
            com.fixcart.fixcart.entity.enums.WorkerType workerType
    ) {
        double[] expansion = new double[]{assignmentRadiusKm, assignmentRadiusKm * 1.5, assignmentRadiusKm * 2};
        for (double radius : expansion) {
            WorkerService.WorkerDistance candidate = workerService.findNearestWorker(latitude, longitude, workerType, radius);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }
}
