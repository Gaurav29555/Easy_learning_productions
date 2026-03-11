package com.fixcart.fixcart.controller;

import com.fixcart.fixcart.dto.NotificationResponse;
import com.fixcart.fixcart.service.NotificationService;
import com.fixcart.fixcart.service.UserService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> myNotifications(Principal principal) {
        Long userId = userService.extractUserId(principal.getName());
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Principal principal) {
        Long userId = userService.extractUserId(principal.getName());
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.unreadCount(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(Principal principal, @PathVariable Long notificationId) {
        Long userId = userService.extractUserId(principal.getName());
        return ResponseEntity.ok(notificationService.markRead(userId, notificationId));
    }
}
