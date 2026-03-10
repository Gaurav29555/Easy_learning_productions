package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.TrackingEventResponse;
import com.fixcart.fixcart.dto.TrackingUpdateRequest;
import com.fixcart.fixcart.entity.Booking;
import com.fixcart.fixcart.entity.TrackingEvent;
import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.exception.BadRequestException;
import com.fixcart.fixcart.exception.ResourceNotFoundException;
import com.fixcart.fixcart.repository.TrackingEventRepository;
import com.fixcart.fixcart.repository.WorkerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final TrackingEventRepository trackingEventRepository;
    private final BookingService bookingService;
    private final WorkerRepository workerRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public TrackingEventResponse publishLocation(Long bookingId, Long userId, UserRole role, TrackingUpdateRequest request) {
        Booking booking = bookingService.findBookingOrThrow(bookingId);

        Worker worker;
        if (role == UserRole.ADMIN) {
            if (booking.getWorker() == null) {
                throw new BadRequestException("Booking has no assigned worker");
            }
            worker = booking.getWorker();
        } else {
            worker = workerRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));
            if (booking.getWorker() == null || !booking.getWorker().getId().equals(worker.getId())) {
                throw new BadRequestException("Worker is not assigned to this booking");
            }
        }

        TrackingEvent event = new TrackingEvent();
        event.setBooking(booking);
        event.setWorker(worker);
        event.setLatitude(request.latitude());
        event.setLongitude(request.longitude());
        event.setSpeedKmh(request.speedKmh());
        TrackingEvent saved = trackingEventRepository.save(event);

        TrackingEventResponse response = toResponse(saved);
        messagingTemplate.convertAndSend("/topic/booking/" + bookingId + "/tracking", response);
        return response;
    }

    public List<TrackingEventResponse> getRecentEvents(Long bookingId, Long userId, UserRole role) {
        Booking booking = bookingService.findBookingOrThrow(bookingId);
        bookingService.validateBookingAccess(booking, userId, role);
        return trackingEventRepository.findTop50ByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    private TrackingEventResponse toResponse(TrackingEvent event) {
        return new TrackingEventResponse(
                event.getBooking().getId(),
                event.getWorker().getId(),
                event.getLatitude(),
                event.getLongitude(),
                event.getSpeedKmh(),
                event.getCreatedAt()
        );
    }
}
