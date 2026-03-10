package com.fixcart.fixcart.controller;

import com.fixcart.fixcart.dto.UpdateWorkerLocationRequest;
import com.fixcart.fixcart.dto.WorkerResponse;
import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.WorkerType;
import com.fixcart.fixcart.service.UserService;
import com.fixcart.fixcart.service.WorkerService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;
    private final UserService userService;

    @GetMapping("/nearby")
    public ResponseEntity<List<WorkerResponse>> findNearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam WorkerType workerType,
            @RequestParam(defaultValue = "20") double radiusKm
    ) {
        return ResponseEntity.ok(workerService.findNearbyWorkers(latitude, longitude, workerType, radiusKm));
    }

    @PatchMapping("/me/location")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<WorkerResponse> updateLocation(
            Principal principal,
            @Valid @RequestBody UpdateWorkerLocationRequest request
    ) {
        Long userId = userService.extractUserId(principal.getName());
        Worker worker = workerService.updateWorkerLocation(userId, request);
        WorkerResponse response = workerService.mapWorkerWithDistance(worker, request.latitude(), request.longitude());
        return ResponseEntity.ok(response);
    }
}
