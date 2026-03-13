package com.fixcart.fixcart.service;

import com.fixcart.fixcart.entity.Booking;
import com.fixcart.fixcart.entity.EtaNotificationSubscription;
import com.fixcart.fixcart.entity.User;
import com.fixcart.fixcart.exception.ResourceNotFoundException;
import com.fixcart.fixcart.repository.EtaNotificationSubscriptionRepository;
import com.fixcart.fixcart.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EtaSubscriptionService {

    private final EtaNotificationSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public EtaNotificationSubscription subscribe(Booking booking, Long userId, double thresholdKm) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        EtaNotificationSubscription subscription = subscriptionRepository
                .findByBookingIdAndUserIdAndActiveTrue(booking.getId(), userId)
                .orElseGet(EtaNotificationSubscription::new);
        subscription.setBooking(booking);
        subscription.setUser(user);
        subscription.setThresholdKm(thresholdKm);
        subscription.setActive(true);
        subscription.setNotified(false);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void processDistanceUpdate(Booking booking, double distanceKm, long etaMinutes) {
        List<EtaNotificationSubscription> subscriptions = subscriptionRepository.findByBookingIdAndActiveTrueAndNotifiedFalse(booking.getId());
        for (EtaNotificationSubscription subscription : subscriptions) {
            if (distanceKm <= subscription.getThresholdKm()) {
                notificationService.sendToUser(
                        subscription.getUser().getId(),
                        "WORKER_ETA_THRESHOLD",
                        "Worker ETA update",
                        "Your fixcart worker is within " + String.format("%.2f", distanceKm) + " km. ETA " + etaMinutes + " minutes."
                );
                subscription.setNotified(true);
                subscription.setActive(false);
                subscriptionRepository.save(subscription);
            }
        }
    }
}
