package com.fixcart.fixcart.entity;

import com.fixcart.fixcart.entity.enums.WorkerType;
import com.fixcart.fixcart.entity.enums.WorkerApprovalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fixcart_workers")
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerType workerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerApprovalStatus approvalStatus = WorkerApprovalStatus.PENDING_REVIEW;

    @Column(length = 255)
    private String kycDocumentUrl;

    @Column(nullable = false)
    private int yearsOfExperience = 0;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private boolean available = true;
}
