package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.RoutePointResponse;
import com.fixcart.fixcart.dto.RouteSimulationResponse;
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
import java.util.ArrayList;
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
    private final WorkerService workerService;
    private final NotificationService notificationService;

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
        if (response.distanceToDestinationKm() <= 0.5d) {
            notificationService.sendToUser(
                    booking.getCustomer().getId(),
                    "WORKER_ARRIVING",
                    "Worker is arriving",
                    "Your fixcart worker is within " + String.format("%.2f", response.distanceToDestinationKm()) + " km."
            );
        }
        return response;
    }

    public List<TrackingEventResponse> getRecentEvents(Long bookingId, Long userId, UserRole role) {
        Booking booking = bookingService.findBookingOrThrow(bookingId);
        bookingService.validateBookingAccess(booking, userId, role);
        return trackingEventRepository.findTop50ByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    public RouteSimulationResponse simulateRoute(Long bookingId, Long userId, UserRole role) {
        Booking booking = bookingService.findBookingOrThrow(bookingId);
        bookingService.validateBookingAccess(booking, userId, role);

        double originLatitude;
        double originLongitude;
        List<TrackingEvent> events = trackingEventRepository.findTop50ByBookingIdOrderByCreatedAtDesc(bookingId);
        if (!events.isEmpty()) {
            TrackingEvent latest = events.get(0);
            originLatitude = latest.getLatitude();
            originLongitude = latest.getLongitude();
        } else if (booking.getWorker() != null) {
            originLatitude = booking.getWorker().getLatitude();
            originLongitude = booking.getWorker().getLongitude();
        } else {
            throw new BadRequestException("Booking has no worker route to simulate");
        }

        double destinationLatitude = booking.getCustomerLatitude();
        double destinationLongitude = booking.getCustomerLongitude();
        double totalDistanceKm = workerService.haversineKm(originLatitude, originLongitude, destinationLatitude, destinationLongitude);
        long etaMinutes = estimateEtaMinutes(totalDistanceKm, events.isEmpty() ? 22 : Math.max(events.get(0).getSpeedKmh(), 12));
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

    private TrackingEventResponse toResponse(TrackingEvent event) {
        double distanceToDestinationKm = workerService.haversineKm(
                event.getLatitude(),
                event.getLongitude(),
                event.getBooking().getCustomerLatitude(),
                event.getBooking().getCustomerLongitude()
        );
        long etaMinutes = estimateEtaMinutes(distanceToDestinationKm, event.getSpeedKmh());
        return new TrackingEventResponse(
                event.getBooking().getId(),
                event.getWorker().getId(),
                event.getLatitude(),
                event.getLongitude(),
                event.getSpeedKmh(),
                distanceToDestinationKm,
                etaMinutes,
                event.getCreatedAt()
        );
    }

    private long estimateEtaMinutes(double distanceKm, double speedKmh) {
        double effectiveSpeed = speedKmh > 5 ? speedKmh : 22;
        return Math.max(1L, Math.round((distanceKm / effectiveSpeed) * 60));
    }

    private List<RoutePointResponse> interpolateRoute(double startLat, double startLng, double endLat, double endLng) {
        List<RoutePointResponse> points = new ArrayList<>();
        int segments = 12;
        for (int i = 0; i <= segments; i++) {
            double ratio = i / (double) segments;
            points.add(new RoutePointResponse(
                    startLat + ((endLat - startLat) * ratio),
                    startLng + ((endLng - startLng) * ratio)
            ));
        }
        return points;
    }
}
