package com.fixcart.fixcart.repository;

import com.fixcart.fixcart.entity.TrackingEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    List<TrackingEvent> findTop50ByBookingIdOrderByCreatedAtDesc(Long bookingId);

    TrackingEvent findTop1ByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
