package com.fixcart.fixcart.controller;

import com.fixcart.fixcart.dto.VoiceCommandRequest;
import com.fixcart.fixcart.dto.VoiceCommandResponse;
import com.fixcart.fixcart.service.UserService;
import com.fixcart.fixcart.service.VoiceCommandService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceCommandController {

    private final VoiceCommandService voiceCommandService;
    private final UserService userService;

    @PostMapping("/commands")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<VoiceCommandResponse> handleCommand(
            Principal principal,
            @Valid @RequestBody VoiceCommandRequest request
    ) {
        Long customerId = userService.extractUserId(principal.getName());
        return ResponseEntity.ok(voiceCommandService.handleCustomerCommand(customerId, request));
    }
}
