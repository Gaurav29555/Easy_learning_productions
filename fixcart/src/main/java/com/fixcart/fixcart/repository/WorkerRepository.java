package com.fixcart.fixcart.repository;

import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.WorkerApprovalStatus;
import com.fixcart.fixcart.entity.enums.WorkerType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    List<Worker> findByAvailableTrueAndWorkerTypeAndApprovalStatus(WorkerType workerType, WorkerApprovalStatus approvalStatus);

    Optional<Worker> findByUserId(Long userId);

    long countByAvailableTrue();

    long countByApprovalStatus(WorkerApprovalStatus approvalStatus);
}
