package com.fixcart.fixcart.repository;

import com.fixcart.fixcart.entity.Payment;
import com.fixcart.fixcart.entity.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    long countByStatus(PaymentStatus status);
}
