package com.fixcart.fixcart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fixcart_dispatch_config")
public class DispatchConfiguration {

    @Id
    private Long id;

    @Column(nullable = false)
    private long stalledMinutesThreshold;

    @Column(nullable = false)
    private double regressionDistanceKm;

    @Column(nullable = false)
    private long etaRegressionMinutes;

    @Column(nullable = false)
    private double inactiveSpeedThresholdKmh;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
