package com.fixcart.fixcart.repository;

import com.fixcart.fixcart.entity.Booking;
import com.fixcart.fixcart.entity.enums.BookingStatus;
import com.fixcart.fixcart.entity.enums.WorkerType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByWorkerIdOrderByCreatedAtDesc(Long workerId);

    List<Booking> findTop100ByOrderByCreatedAtDesc();

    Booking findTop1ByCustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByStatus(BookingStatus status);

    long countByStatusIn(List<BookingStatus> statuses);

    long countByWorkerIdAndStatusIn(Long workerId, List<BookingStatus> statuses);

    long countByWorkerIsNotNull();

    long countByServiceType(WorkerType serviceType);
}
