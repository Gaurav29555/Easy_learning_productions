package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.AddressSuggestionResponse;
import com.fixcart.fixcart.dto.CreateBookingRequest;
import com.fixcart.fixcart.dto.VoiceCommandRequest;
import com.fixcart.fixcart.dto.VoiceCommandResponse;
import com.fixcart.fixcart.entity.Booking;
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
    private final AddressSearchService addressSearchService;
    private final VoiceIntentParserService voiceIntentParserService;
    private final com.fixcart.fixcart.repository.BookingRepository bookingRepository;

    public VoiceCommandResponse handleCustomerCommand(Long customerId, VoiceCommandRequest request) {
        var intent = voiceIntentParserService.parse(request.transcript(), request.languageCode(), request.serviceAddress());
        WorkerType detectedType = intent.workerType();
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Say: hello fixcart book plumber for me");
        suggestions.add("Say: find nearest electrician near me");
        suggestions.add("Say: book AC repair at Baner Pune tomorrow evening");
        suggestions.add("Say: fixcart mera booking status batao");
        suggestions.add("Say: cancel my latest booking");
        suggestions.add("Say: track my worker");

        if (intent.action() == VoiceIntentParserService.VoiceAction.STATUS) {
            Booking latestBooking = bookingRepository.findTop1ByCustomerIdOrderByCreatedAtDesc(customerId);
            if (latestBooking == null) {
                throw new BadRequestException("No fixcart booking exists yet for this account.");
            }
            var response = bookingService.toResponse(latestBooking);
            String spoken = "Your latest fixcart booking status is " + latestBooking.getStatus().name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".";
            return new VoiceCommandResponse("BOOKING_STATUS", spoken, response, null, List.of(), List.of(), suggestions);
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.CANCEL) {
            Booking latestBooking = requireActionableBooking(customerId);
            var response = bookingService.updateStatus(latestBooking.getId(), BookingStatus.CANCELLED, "Cancelled by fixcart voice assistant", customerId, UserRole.CUSTOMER);
            return new VoiceCommandResponse("BOOKING_CANCELLED", "Your latest active fixcart booking has been cancelled.", response, null, List.of(), List.of(), suggestions);
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.RESCHEDULE) {
            Booking latestBooking = requireActionableBooking(customerId);
            if (intent.requestedSchedule() == null) {
                throw new BadRequestException("Say when you want to reschedule, for example tomorrow morning.");
            }
            var response = bookingService.rescheduleBooking(latestBooking.getId(), intent.requestedSchedule(), customerId, UserRole.CUSTOMER);
            String spoken = "Your latest fixcart booking was rescheduled for " + intent.requestedSchedule().toLocalDate() + " at " + intent.requestedSchedule().toLocalTime() + ".";
            return new VoiceCommandResponse("BOOKING_RESCHEDULED", spoken, response, null, List.of(), List.of(), suggestions);
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
                    suggestions
            );
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.FIND_NEARBY) {
            if (detectedType == null) {
                throw new BadRequestException("Voice command should mention a service like plumber, carpenter or electrician.");
            }
            AddressSuggestionResponse resolvedAddress = resolveAddress(intent.addressHint(), request.serviceAddress(), request.latitude(), request.longitude());
            double latitude = resolvedAddress == null ? requireLatitude(request.latitude()) : resolvedAddress.latitude();
            double longitude = resolvedAddress == null ? requireLongitude(request.longitude()) : resolvedAddress.longitude();
            var workers = workerService.findNearbyWorkers(latitude, longitude, detectedType, 20);
            String spoken = workers.isEmpty()
                    ? "No nearby " + formatType(detectedType) + " workers are currently available."
                    : "I found " + workers.size() + " nearby " + formatType(detectedType) + " workers.";
            return new VoiceCommandResponse(
                    "FIND_NEARBY",
                    spoken,
                    null,
                    null,
                    workers,
                    resolvedAddress == null ? addressSuggestionList(intent.addressHint(), request.latitude(), request.longitude()) : List.of(resolvedAddress),
                    suggestions
            );
        }

        if (intent.action() == VoiceIntentParserService.VoiceAction.BOOK) {
            if (detectedType == null) {
                throw new BadRequestException("Voice booking command should mention the worker type you need.");
            }
            AddressSuggestionResponse resolvedAddress = resolveAddress(intent.addressHint(), request.serviceAddress(), request.latitude(), request.longitude());
            double latitude = resolvedAddress == null ? requireLatitude(request.latitude()) : resolvedAddress.latitude();
            double longitude = resolvedAddress == null ? requireLongitude(request.longitude()) : resolvedAddress.longitude();
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
            return new VoiceCommandResponse(
                    "BOOKING_CREATED",
                    spoken,
                    booking,
                    null,
                    List.of(),
                    resolvedAddress == null ? addressSuggestionList(intent.addressHint(), request.latitude(), request.longitude()) : List.of(resolvedAddress),
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
        return addressSearchService.resolveBestMatch(candidate, latitude, longitude);
    }

    private List<AddressSuggestionResponse> addressSuggestionList(String addressHint, Double latitude, Double longitude) {
        if (addressHint == null || addressHint.isBlank()) {
            return List.of();
        }
        return addressSearchService.search(addressHint, latitude, longitude);
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
