package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.AddressSuggestionResponse;
import com.fixcart.fixcart.dto.CreateBookingRequest;
import com.fixcart.fixcart.dto.VoiceCommandRequest;
import com.fixcart.fixcart.dto.VoiceCommandResponse;
import com.fixcart.fixcart.entity.Booking;
import com.fixcart.fixcart.entity.VoiceConversationMemory;
import com.fixcart.fixcart.entity.enums.BookingStatus;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.entity.enums.WorkerType;
import com.fixcart.fixcart.exception.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoiceCommandService {

    private final BookingService bookingService;
    private final WorkerService workerService;
    private final TrackingService trackingService;
    private final GeocodingService geocodingService;
    private final VoiceIntentParserService voiceIntentParserService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final VoiceConversationMemoryService voiceConversationMemoryService;
    private final EtaSubscriptionService etaSubscriptionService;
    private final com.fixcart.fixcart.repository.BookingRepository bookingRepository;

    public VoiceCommandResponse handleCustomerCommand(Long customerId, VoiceCommandRequest request) {
        var intent = voiceIntentParserService.parse(request.transcript(), request.languageCode(), request.serviceAddress());
        VoiceConversationMemory memory = voiceConversationMemoryService.getActiveMemory(customerId);
        WorkerType detectedType = intent.workerType() != null ? intent.workerType()
                : (voiceIntentParserService.isFollowUpReference(intent.normalizedTranscript()) && memory != null ? memory.getLastWorkerType() : null);
        String effectiveAddressHint = intent.addressHint() != null ? intent.addressHint()
                : (memory != null ? memory.getLastAddressHint() : null);
        Double effectiveLatitude = request.latitude() != null ? request.latitude() : (memory != null ? memory.getLastLatitude() : null);
        Double effectiveLongitude = request.longitude() != null ? request.longitude() : (memory != null ? memory.getLastLongitude() : null);
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Say: hello fixcart book plumber for me");
        suggestions.add("Say: find nearest electrician near me");
        suggestions.add("Say: book AC repair at Baner Pune tomorrow evening");
        suggestions.add("Say: fixcart mera booking status batao");
        suggestions.add("Say: cancel my latest booking");
        suggestions.add("Say: track my worker");
        suggestions.add("Say: pay for my latest booking");
        suggestions.add("Say: connect me to support");
        suggestions.add("Say: notify me when worker is arriving");

        if (intent.action() == VoiceIntentParserService.VoiceAction.STATUS) {
            Booking latestBooking = bookingRepository.findTop1ByCustomerIdOrderByCreatedAtDesc(customerId);
            if (latestBooking == null) {
                throw new BadRequestException("No fixcart booking exists yet for this account.");
            }
            var response = bookingService.toResponse(latestBooking);
            String spoken = "Your latest fixcart booking status is " + latestBooking.getStatus().name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".";
            voiceConversationMemoryService.remember(customerId, "BOOKING_STATUS", latestBooking.getServiceType(), latestBooking.getServiceAddress(), latestBooking.getCustomerLatitude(), latestBooking.getCustomerLongitude(), latestBooking.getId());
            return new VoiceCommandResponse("BOOKING_STATUS", spoken, response, null, List.of(), List.of(), null, suggestions);
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.CANCEL) {
            Booking latestBooking = requireActionableBooking(customerId);
            var response = bookingService.updateStatus(latestBooking.getId(), BookingStatus.CANCELLED, "Cancelled by fixcart voice assistant", customerId, UserRole.CUSTOMER);
            voiceConversationMemoryService.remember(customerId, "BOOKING_CANCELLED", latestBooking.getServiceType(), latestBooking.getServiceAddress(), latestBooking.getCustomerLatitude(), latestBooking.getCustomerLongitude(), latestBooking.getId());
            return new VoiceCommandResponse("BOOKING_CANCELLED", "Your latest active fixcart booking has been cancelled.", response, null, List.of(), List.of(), null, suggestions);
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.RESCHEDULE) {
            Booking latestBooking = requireActionableBooking(customerId);
            if (intent.requestedSchedule() == null) {
                throw new BadRequestException("Say when you want to reschedule, for example tomorrow morning.");
            }
            var response = bookingService.rescheduleBooking(latestBooking.getId(), intent.requestedSchedule(), customerId, UserRole.CUSTOMER);
            String spoken = "Your latest fixcart booking was rescheduled for " + intent.requestedSchedule().toLocalDate() + " at " + intent.requestedSchedule().toLocalTime() + ".";
            voiceConversationMemoryService.remember(customerId, "BOOKING_RESCHEDULED", latestBooking.getServiceType(), latestBooking.getServiceAddress(), latestBooking.getCustomerLatitude(), latestBooking.getCustomerLongitude(), latestBooking.getId());
            return new VoiceCommandResponse("BOOKING_RESCHEDULED", spoken, response, null, List.of(), List.of(), null, suggestions);
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.TRACK) {
            Booking latestBooking = requireActionableBooking(customerId);
            if (latestBooking.getWorker() == null) {
                throw new BadRequestException("No worker is assigned yet, so live tracking is not available.");
            }
            var route = trackingService.simulateRoute(latestBooking.getId(), customerId, UserRole.CUSTOMER);
            var response = bookingService.toResponse(latestBooking);
            return new VoiceCommandResponse(
                    "TRACK_WORKER",
                    "Your worker is about " + route.etaMinutes() + " minutes away.",
                    response,
                    route,
                    List.of(),
                    List.of(),
                    null,
                    suggestions
            );
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.ETA_NOTIFY) {
            Booking latestBooking = requireActionableBooking(customerId);
            if (latestBooking.getWorker() == null) {
                throw new BadRequestException("No worker is assigned yet, so ETA notification cannot be created.");
            }
            var route = trackingService.simulateRoute(latestBooking.getId(), customerId, UserRole.CUSTOMER);
            double thresholdKm = intent.etaThresholdKm() != null ? intent.etaThresholdKm() : 2.0d;
            etaSubscriptionService.subscribe(latestBooking, customerId, thresholdKm);
            var response = bookingService.toResponse(latestBooking);
            voiceConversationMemoryService.remember(customerId, "ETA_NOTIFY", latestBooking.getServiceType(), latestBooking.getServiceAddress(), latestBooking.getCustomerLatitude(), latestBooking.getCustomerLongitude(), latestBooking.getId());
            return new VoiceCommandResponse(
                    "ETA_NOTIFY",
                    "I subscribed you to a fixcart arrival alert. You will be notified when the worker is within " + thresholdKm + " kilometers.",
                    response,
                    route,
                    List.of(),
                    List.of(),
                    thresholdKm,
                    suggestions
            );
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.PAYMENT) {
            Booking latestBooking = requireActionableBooking(customerId);
            var payment = paymentService.createVoiceOrderForBooking(customerId, latestBooking.getId());
            var response = bookingService.toResponse(latestBooking);
            String spoken = "A payment order was created for your latest fixcart booking. Amount is "
                    + payment.amount() + " " + payment.currency() + ".";
            voiceConversationMemoryService.remember(customerId, "PAYMENT_ORDER_CREATED", latestBooking.getServiceType(), latestBooking.getServiceAddress(), latestBooking.getCustomerLatitude(), latestBooking.getCustomerLongitude(), latestBooking.getId());
            return new VoiceCommandResponse("PAYMENT_ORDER_CREATED", spoken, response, null, List.of(), List.of(), null, suggestions);
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.ESCALATE) {
            Booking latestBooking = requireActionableBooking(customerId);
            notificationService.sendToRole(
                    UserRole.ADMIN,
                    "CUSTOMER_ESCALATION",
                    "Customer requested help",
                    "fixcart customer #" + customerId + " requested admin support for booking #" + latestBooking.getId() + "."
            );
            auditLogService.record(
                    com.fixcart.fixcart.entity.enums.AuditActionType.BOOKING_STATUS_UPDATED,
                    "USER",
                    customerId,
                    "BOOKING",
                    latestBooking.getId(),
                    "Customer escalated booking to admin via voice assistant"
            );
            var response = bookingService.toResponse(latestBooking);
            return new VoiceCommandResponse(
                    "ADMIN_ESCALATION",
                    "I sent your issue to the fixcart support team. An admin can now review your booking.",
                    response,
                    null,
                    List.of(),
                    List.of(),
                    null,
                    suggestions
            );
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.FIND_NEARBY) {
            if (detectedType == null) {
                throw new BadRequestException("Voice command should mention a service like plumber, carpenter or electrician.");
            }
            AddressSuggestionResponse resolvedAddress = resolveAddress(effectiveAddressHint, request.serviceAddress(), effectiveLatitude, effectiveLongitude);
            double latitude = resolvedAddress == null ? requireLatitude(effectiveLatitude) : resolvedAddress.latitude();
            double longitude = resolvedAddress == null ? requireLongitude(effectiveLongitude) : resolvedAddress.longitude();
            var workers = workerService.findNearbyWorkers(latitude, longitude, detectedType, 20);
            String spoken = workers.isEmpty()
                    ? "No nearby " + formatType(detectedType) + " workers are currently available."
                    : "I found " + workers.size() + " nearby " + formatType(detectedType) + " workers.";
            voiceConversationMemoryService.remember(
                    customerId,
                    "FIND_NEARBY",
                    detectedType,
                    resolvedAddress == null ? effectiveAddressHint : resolvedAddress.label(),
                    latitude,
                    longitude,
                    memory != null ? memory.getLastBookingId() : null
            );
            return new VoiceCommandResponse(
                    "FIND_NEARBY",
                    spoken,
                    null,
                    null,
                    workers,
                    resolvedAddress == null ? addressSuggestionList(effectiveAddressHint, effectiveLatitude, effectiveLongitude) : List.of(resolvedAddress),
                    null,
                    suggestions
            );
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.BOOK) {
            if (detectedType == null) {
                throw new BadRequestException("Voice booking command should mention the worker type you need.");
            }
            AddressSuggestionResponse resolvedAddress = resolveAddress(effectiveAddressHint, request.serviceAddress(), effectiveLatitude, effectiveLongitude);
            double latitude = resolvedAddress == null ? requireLatitude(effectiveLatitude) : resolvedAddress.latitude();
            double longitude = resolvedAddress == null ? requireLongitude(effectiveLongitude) : resolvedAddress.longitude();
            String address = resolvedAddress == null
                    ? (request.serviceAddress() != null && !request.serviceAddress().isBlank() ? request.serviceAddress() : "Voice-requested location")
                    : resolvedAddress.label();

            var booking = bookingService.createBooking(customerId, new CreateBookingRequest(
                    detectedType,
                    address,
                    latitude,
                    longitude,
                    intent.requestedSchedule(),
                    "Created by fixcart voice assistant from transcript: " + request.transcript()
            ));
            String spoken = booking.workerId() == null
                    ? "Your " + formatType(detectedType) + " booking is created. No worker is assigned yet, but fixcart is still searching."
                    : "Your " + formatType(detectedType) + " booking is created and a worker was assigned.";
            voiceConversationMemoryService.remember(customerId, "BOOKING_CREATED", detectedType, address, latitude, longitude, booking.bookingId());
            return new VoiceCommandResponse(
                    "BOOKING_CREATED",
                    spoken,
                    booking,
                    null,
                    List.of(),
                    resolvedAddress == null ? addressSuggestionList(effectiveAddressHint, effectiveLatitude, effectiveLongitude) : List.of(resolvedAddress),
                    null,
                    suggestions
            );
        }

        throw new BadRequestException("Voice command not understood. Try saying book plumber for me, cancel my booking or track my worker.");
    }

    private Booking requireActionableBooking(Long customerId) {
        Booking latestBooking = bookingService.findLatestActionableBooking(customerId);
        if (latestBooking == null) {
            throw new BadRequestException("No active fixcart booking is available for this voice action.");
        }
        return latestBooking;
    }

    private AddressSuggestionResponse resolveAddress(
            String addressHint,
            String serviceAddress,
            Double latitude,
            Double longitude
    ) {
        String candidate = addressHint != null && !addressHint.isBlank() ? addressHint : serviceAddress;
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        return geocodingService.resolveBestMatch(candidate, latitude, longitude);
    }

    private List<AddressSuggestionResponse> addressSuggestionList(String addressHint, Double latitude, Double longitude) {
        if (addressHint == null || addressHint.isBlank()) {
            return List.of();
        }
        return geocodingService.search(addressHint, latitude, longitude);
    }

    private String formatType(WorkerType workerType) {
        return workerType.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private double requireLatitude(Double latitude) {
        if (latitude == null) throw new BadRequestException("Current location is required for voice automation.");
        return latitude;
    }

    private double requireLongitude(Double longitude) {
        if (longitude == null) throw new BadRequestException("Current location is required for voice automation.");
        return longitude;
    }
}
