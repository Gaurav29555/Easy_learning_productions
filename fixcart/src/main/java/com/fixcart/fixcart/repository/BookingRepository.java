package com.fixcart.fixcart.repository;

import com.fixcart.fixcart.entity.Booking;
import com.fixcart.fixcart.entity.enums.BookingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByWorkerIdOrderByCreatedAtDesc(Long workerId);

    List<Booking> findTop100ByOrderByCreatedAtDesc();

    long countByStatus(BookingStatus status);

    long countByWorkerIdAndStatusIn(Long workerId, List<BookingStatus> statuses);
}
