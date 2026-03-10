package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.UpdateWorkerLocationRequest;
import com.fixcart.fixcart.dto.WorkerResponse;
import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.BookingStatus;
import com.fixcart.fixcart.entity.enums.WorkerApprovalStatus;
import com.fixcart.fixcart.entity.enums.WorkerType;
import com.fixcart.fixcart.exception.ResourceNotFoundException;
import com.fixcart.fixcart.repository.BookingRepository;
import com.fixcart.fixcart.repository.WorkerRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final BookingRepository bookingRepository;

    public List<WorkerResponse> findNearbyWorkers(double latitude, double longitude, WorkerType workerType, double radiusKm) {
        return workerRepository.findByAvailableTrueAndWorkerTypeAndApprovalStatus(workerType, WorkerApprovalStatus.APPROVED).stream()
                .map(worker -> mapWorkerWithDistance(worker, latitude, longitude))
                .filter(worker -> worker.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(WorkerResponse::distanceKm))
                .toList();
    }

    public WorkerDistance findNearestWorker(double latitude, double longitude, WorkerType workerType, double radiusKm) {
        List<BookingStatus> activeStatuses = Arrays.asList(BookingStatus.ASSIGNED, BookingStatus.IN_PROGRESS);
        return workerRepository.findByAvailableTrueAndWorkerTypeAndApprovalStatus(workerType, WorkerApprovalStatus.APPROVED).stream()
                .map(worker -> {
                    double distance = haversineKm(latitude, longitude, worker.getLatitude(), worker.getLongitude());
                    long activeBookings = bookingRepository.countByWorkerIdAndStatusIn(worker.getId(), activeStatuses);
                    // Weighted score balances distance and current worker load.
                    double score = distance + (activeBookings * 1.5);
                    return new WorkerDistance(worker, distance, activeBookings, score);
                })
                .filter(workerDistance -> workerDistance.distanceKm <= radiusKm)
                .min(Comparator.comparingDouble(WorkerDistance::getAssignmentScore))
                .orElse(null);
    }

    @Transactional
    public Worker updateWorkerLocation(Long userId, UpdateWorkerLocationRequest request) {
        Worker worker = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));

        worker.setLatitude(request.latitude());
        worker.setLongitude(request.longitude());
        return workerRepository.save(worker);
    }

    public WorkerResponse mapWorkerWithDistance(Worker worker, double latitude, double longitude) {
        double distance = haversineKm(latitude, longitude, worker.getLatitude(), worker.getLongitude());
        return new WorkerResponse(
                worker.getId(),
                worker.getUser().getId(),
                worker.getUser().getFullName(),
                worker.getWorkerType(),
                worker.getApprovalStatus(),
                worker.getKycDocumentUrl(),
                worker.getYearsOfExperience(),
                worker.getLatitude(),
                worker.getLongitude(),
                worker.isAvailable(),
                distance
        );
    }

    public double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    @Getter
    public static class WorkerDistance {
        private final Worker worker;
        private final double distanceKm;
        private final long activeBookings;
        private final double assignmentScore;

        public WorkerDistance(Worker worker, double distanceKm, long activeBookings, double assignmentScore) {
            this.worker = worker;
            this.distanceKm = distanceKm;
            this.activeBookings = activeBookings;
            this.assignmentScore = assignmentScore;
        }
    }
}
