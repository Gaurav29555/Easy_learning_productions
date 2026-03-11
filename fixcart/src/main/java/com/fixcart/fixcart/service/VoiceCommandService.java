package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.CreateBookingRequest;
import com.fixcart.fixcart.dto.VoiceCommandRequest;
import com.fixcart.fixcart.dto.VoiceCommandResponse;
import com.fixcart.fixcart.entity.Booking;
import com.fixcart.fixcart.entity.enums.WorkerType;
import com.fixcart.fixcart.exception.BadRequestException;
import java.time.LocalDateTime;
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
    private final com.fixcart.fixcart.repository.BookingRepository bookingRepository;

    public VoiceCommandResponse handleCustomerCommand(Long customerId, VoiceCommandRequest request) {
        String normalized = request.transcript().trim().toLowerCase(Locale.ROOT);
        WorkerType detectedType = detectWorkerType(normalized);
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Say: hello fixcart book plumber for me");
        suggestions.add("Say: find nearest electrician near me");
        suggestions.add("Say: book AC repair at my current location");
        suggestions.add("Say: fixcart mera booking status batao");

        if (normalized.contains("status") || normalized.contains("booking status") || normalized.contains("batao") || normalized.contains("mera booking")) {
            Booking latestBooking = bookingRepository.findTop1ByCustomerIdOrderByCreatedAtDesc(customerId);
            if (latestBooking == null) {
                throw new BadRequestException("No fixcart booking exists yet for this account.");
            }
            var response = bookingService.toResponse(latestBooking);
            String spoken = "Your latest fixcart booking status is " + latestBooking.getStatus().name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".";
            return new VoiceCommandResponse("BOOKING_STATUS", spoken, response, List.of(), suggestions);
        }

        if (normalized.contains("nearest") || normalized.contains("nearby") || normalized.contains("find") || normalized.contains("paas") || normalized.contains("near me")) {
            if (detectedType == null) {
                throw new BadRequestException("Voice command should mention a service like plumber, carpenter or electrician.");
            }
            double latitude = requireLatitude(request.latitude());
            double longitude = requireLongitude(request.longitude());
            var workers = workerService.findNearbyWorkers(latitude, longitude, detectedType, 20);
            String spoken = workers.isEmpty()
                    ? "No nearby " + formatType(detectedType) + " workers are currently available."
                    : "I found " + workers.size() + " nearby " + formatType(detectedType) + " workers.";
            return new VoiceCommandResponse("FIND_NEARBY", spoken, null, workers, suggestions);
        }

        if (normalized.contains("book")
                || normalized.contains("assign")
                || normalized.contains("worker for me")
                || normalized.contains("chahiye")
                || normalized.contains("book karo")
                || normalized.contains("bhejo")) {
            if (detectedType == null) {
                throw new BadRequestException("Voice booking command should mention the worker type you need.");
            }
            double latitude = requireLatitude(request.latitude());
            double longitude = requireLongitude(request.longitude());
            String address = request.serviceAddress() != null && !request.serviceAddress().isBlank()
                    ? request.serviceAddress()
                    : "Voice-requested location";
            var booking = bookingService.createBooking(customerId, new CreateBookingRequest(
                    detectedType,
                    address,
                    latitude,
                    longitude,
                    normalized.contains("tomorrow") ? LocalDateTime.now().plusDays(1).withHour(10).withMinute(0) : null,
                    "Created by fixcart voice assistant from transcript: " + request.transcript()
            ));
            String spoken = booking.workerId() == null
                    ? "Your " + formatType(detectedType) + " booking is created. No worker is assigned yet, but fixcart is still searching."
                    : "Your " + formatType(detectedType) + " booking is created and a worker was assigned.";
            return new VoiceCommandResponse("BOOKING_CREATED", spoken, booking, List.of(), suggestions);
        }

        throw new BadRequestException("Voice command not understood. Try saying book plumber for me or find nearest electrician.");
    }

    private WorkerType detectWorkerType(String normalized) {
        if (normalized.contains("plumber") || normalized.contains("pipe") || normalized.contains("leak")) return WorkerType.PLUMBER;
        if (normalized.contains("carpenter") || normalized.contains("wood") || normalized.contains("furniture")) return WorkerType.CARPENTER;
        if (normalized.contains("electrician") || normalized.contains("electric") || normalized.contains("light") || normalized.contains("switch")) return WorkerType.ELECTRICIAN;
        if (normalized.contains("cleaner") || normalized.contains("cleaning") || normalized.contains("safai")) return WorkerType.CLEANER;
        if (normalized.contains("ac") || normalized.contains("air conditioner")) return WorkerType.AC_REPAIR;
        if (normalized.contains("appliance") || normalized.contains("fridge") || normalized.contains("washing machine") || normalized.contains("machine")) return WorkerType.APPLIANCE_REPAIR;
        if (normalized.contains("paint") || normalized.contains("painter") || normalized.contains("deewar")) return WorkerType.PAINTER;
        return null;
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
