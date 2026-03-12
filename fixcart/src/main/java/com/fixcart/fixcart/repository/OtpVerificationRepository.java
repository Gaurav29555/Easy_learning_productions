package com.fixcart.fixcart.repository;

import com.fixcart.fixcart.entity.OtpVerification;
import com.fixcart.fixcart.entity.enums.OtpPurpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findFirstByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);
}
