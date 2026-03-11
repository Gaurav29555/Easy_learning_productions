package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.BookingRealtimeEvent;
import com.fixcart.fixcart.dto.BookingResponse;
import com.fixcart.fixcart.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public void publish(String eventType, String message, Booking booking) {
        BookingResponse response = new BookingResponse(
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
        BookingRealtimeEvent event = new BookingRealtimeEvent(eventType, message, response);
        messagingTemplate.convertAndSend("/topic/admin/bookings", event);
        messagingTemplate.convertAndSend("/topic/customer/" + booking.getCustomer().getId() + "/bookings", event);
        notificationService.sendToUser(booking.getCustomer().getId(), eventType, "Booking update", message);
        if (booking.getWorker() != null) {
            messagingTemplate.convertAndSend("/topic/worker/" + booking.getWorker().getUser().getId() + "/bookings", event);
            notificationService.sendToUser(booking.getWorker().getUser().getId(), eventType, "New fixcart job update", message);
        }
    }
}
