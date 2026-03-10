package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.BookingResponse;
import com.fixcart.fixcart.dto.CreateBookingRequest;
import com.fixcart.fixcart.entity.Booking;
import com.fixcart.fixcart.entity.User;
import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.BookingStatus;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.exception.BadRequestException;
import com.fixcart.fixcart.exception.ResourceNotFoundException;
import com.fixcart.fixcart.repository.BookingRepository;
import com.fixcart.fixcart.repository.UserRepository;
import com.fixcart.fixcart.repository.WorkerRepository;
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

    @Value("${fixcart.booking.assignment-radius-km}")
    private double assignmentRadiusKm;

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
        booking.setNotes(request.notes());
        booking.setStatus(BookingStatus.PENDING);

        WorkerService.WorkerDistance nearest = findBestWorkerWithRadiusExpansion(
                request.customerLatitude(),
                request.customerLongitude(),
                request.serviceType()
        );

        if (nearest != null) {
            Worker worker = nearest.getWorker();
            worker.setAvailable(false);
            workerRepository.save(worker);
            booking.setWorker(worker);
            booking.setStatus(BookingStatus.ASSIGNED);
        }

        return toResponse(bookingRepository.save(booking));
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
            return toResponse(bookingRepository.save(booking));
        }

        Worker worker = nearest.getWorker();
        worker.setAvailable(false);
        workerRepository.save(worker);

        booking.setWorker(worker);
        booking.setStatus(BookingStatus.ASSIGNED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse updateStatus(Long bookingId, BookingStatus status, Long userId, UserRole role) {
        Booking booking = findBookingOrThrow(bookingId);
        validateBookingAccess(booking, userId, role);

        if (status == BookingStatus.ASSIGNED || status == BookingStatus.PENDING) {
            throw new BadRequestException("Use assignment flow for PENDING or ASSIGNED status");
        }

        if (status == BookingStatus.CANCELLED && role == UserRole.WORKER) {
            throw new BadRequestException("Worker cannot cancel booking");
        }

        booking.setStatus(status);
        Booking saved = bookingRepository.save(booking);

        if (status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED) {
            Worker worker = saved.getWorker();
            if (worker != null) {
                worker.setAvailable(true);
                workerRepository.save(worker);
            }
        }

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
                booking.getNotes(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
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
