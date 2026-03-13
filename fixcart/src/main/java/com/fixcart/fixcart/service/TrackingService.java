package com.fixcart.fixcart.service;

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
import java.time.Duration;
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
    private final RoutePlanningService routePlanningService;
    private final EtaSubscriptionService etaSubscriptionService;

    @org.springframework.beans.factory.annotation.Value("${fixcart.dispatch.stalled-minutes:8}")
    private long stalledMinutesThreshold;

    @org.springframework.beans.factory.annotation.Value("${fixcart.dispatch.regression-km:0.75}")
    private double regressionDistanceKm;

    @org.springframework.beans.factory.annotation.Value("${fixcart.dispatch.eta-regression-minutes:8}")
    private long etaRegressionMinutes;

    @Transactional
    public TrackingEventResponse publishLocation(Long bookingId, Long userId, UserRole role, TrackingUpdateRequest request) {
        Booking booking = bookingService.findBookingOrThrow(bookingId);
        TrackingEvent previousEvent = trackingEventRepository.findTop1ByBookingIdOrderByCreatedAtDesc(bookingId);

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
        etaSubscriptionService.processDistanceUpdate(booking, response.distanceToDestinationKm(), response.etaMinutes());
        evaluateReDispatch(booking, previousEvent, saved, response);
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

        return routePlanningService.simulateBookingRoute(
                bookingId,
                originLatitude,
                originLongitude,
                booking.getCustomerLatitude(),
                booking.getCustomerLongitude(),
                events.isEmpty() ? 22 : Math.max(events.get(0).getSpeedKmh(), 12)
        );
    }

    private TrackingEventResponse toResponse(TrackingEvent event) {
        double distanceToDestinationKm = workerService.haversineKm(
                event.getLatitude(),
                event.getLongitude(),
                event.getBooking().getCustomerLatitude(),
                event.getBooking().getCustomerLongitude()
        );
        long etaMinutes = routePlanningService.estimateEtaMinutes(distanceToDestinationKm, event.getSpeedKmh());
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

    private void evaluateReDispatch(Booking booking, TrackingEvent previousEvent, TrackingEvent currentEvent, TrackingEventResponse currentResponse) {
        if (previousEvent == null || booking.getWorker() == null) {
            return;
        }
        if (booking.getStatus() != com.fixcart.fixcart.entity.enums.BookingStatus.ASSIGNED
                && booking.getStatus() != com.fixcart.fixcart.entity.enums.BookingStatus.IN_PROGRESS) {
            return;
        }

        double previousDistanceKm = workerService.haversineKm(
                previousEvent.getLatitude(),
                previousEvent.getLongitude(),
                booking.getCustomerLatitude(),
                booking.getCustomerLongitude()
        );
        long previousEtaMinutes = routePlanningService.estimateEtaMinutes(previousDistanceKm, previousEvent.getSpeedKmh());
        long gapMinutes = Math.max(1L, Duration.between(previousEvent.getCreatedAt(), currentEvent.getCreatedAt()).toMinutes());

        boolean stalled = currentEvent.getSpeedKmh() < 3.0d
                && gapMinutes >= stalledMinutesThreshold
                && currentResponse.distanceToDestinationKm() >= previousDistanceKm - 0.2d;
        boolean etaWorsened = currentResponse.distanceToDestinationKm() > previousDistanceKm + regressionDistanceKm
                || currentResponse.etaMinutes() > previousEtaMinutes + etaRegressionMinutes;

        if (!stalled && !etaWorsened) {
            return;
        }

        Long previousWorkerId = booking.getWorker().getId();
        var reassigned = bookingService.assignNearestWorker(booking.getId(), previousWorkerId);
        if (reassigned.workerId() != null && !reassigned.workerId().equals(previousWorkerId)) {
            notificationService.sendToUser(
                    booking.getCustomer().getId(),
                    "BOOKING_REDISPATCHED",
                    "Worker reassigned",
                    "fixcart reassigned your booking because the route ETA worsened or movement stalled."
            );
        }
    }
}
