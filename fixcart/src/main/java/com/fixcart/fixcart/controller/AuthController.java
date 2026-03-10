package com.fixcart.fixcart.controller;

import com.fixcart.fixcart.dto.AuthResponse;
import com.fixcart.fixcart.dto.LoginRequest;
import com.fixcart.fixcart.dto.OtpLoginRequest;
import com.fixcart.fixcart.dto.OtpStatusResponse;
import com.fixcart.fixcart.dto.RegisterUserRequest;
import com.fixcart.fixcart.dto.RegisterWorkerRequest;
import com.fixcart.fixcart.dto.SendOtpRequest;
import com.fixcart.fixcart.dto.VerifyOtpRequest;
import com.fixcart.fixcart.service.AuthService;
import com.fixcart.fixcart.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/otp/send")
    public ResponseEntity<OtpStatusResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(otpService.sendOtp(request.phone(), request.purpose()));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<OtpStatusResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(otpService.verifyOtp(request.phone(), request.purpose(), request.otpCode()));
    }

    @PostMapping("/register/user")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCustomer(request));
    }

    @PostMapping("/register/worker")
    public ResponseEntity<AuthResponse> registerWorker(@Valid @RequestBody RegisterWorkerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerWorker(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/login/otp")
    public ResponseEntity<AuthResponse> loginWithOtp(@Valid @RequestBody OtpLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithOtp(request));
    }
}
