package com.fixcart.fixcart.repository;

import com.fixcart.fixcart.entity.Payment;
import com.fixcart.fixcart.entity.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    long countByStatus(PaymentStatus status);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = com.fixcart.fixcart.entity.enums.PaymentStatus.SUCCESS")
    BigDecimal sumSuccessfulPayments();
}
