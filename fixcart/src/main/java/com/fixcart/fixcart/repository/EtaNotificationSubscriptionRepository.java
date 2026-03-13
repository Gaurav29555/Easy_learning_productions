package com.fixcart.fixcart.repository;

import com.fixcart.fixcart.entity.EtaNotificationSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtaNotificationSubscriptionRepository extends JpaRepository<EtaNotificationSubscription, Long> {

    List<EtaNotificationSubscription> findByBookingIdAndActiveTrueAndNotifiedFalse(Long bookingId);

    Optional<EtaNotificationSubscription> findByBookingIdAndUserIdAndActiveTrue(Long bookingId, Long userId);
}
