package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.AuthResponse;
import com.fixcart.fixcart.dto.LoginRequest;
import com.fixcart.fixcart.dto.OtpLoginRequest;
import com.fixcart.fixcart.dto.RegisterUserRequest;
import com.fixcart.fixcart.dto.RegisterWorkerRequest;
import com.fixcart.fixcart.entity.User;
import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.AuditActionType;
import com.fixcart.fixcart.entity.enums.OtpPurpose;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.entity.enums.WorkerApprovalStatus;
import com.fixcart.fixcart.exception.BadRequestException;
import com.fixcart.fixcart.repository.UserRepository;
import com.fixcart.fixcart.repository.WorkerRepository;
import com.fixcart.fixcart.security.JwtService;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final AuditLogService auditLogService;
    private final RequestRateLimitService requestRateLimitService;

    @Transactional
    public AuthResponse registerCustomer(RegisterUserRequest request) {
        otpService.assertEmailVerifiedForPurpose(request.email(), OtpPurpose.REGISTER);
        validateUniqueFields(request.email(), request.phone());

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(UserRole.CUSTOMER);
        User saved = userRepository.save(user);
        otpService.consumeVerifiedOtp(request.email(), OtpPurpose.REGISTER);
        auditLogService.record(AuditActionType.USER_REGISTERED, "USER", saved.getId(), "USER", saved.getId(), "Customer registered in fixcart");

        String token = createToken(saved);
        return new AuthResponse(token, saved.getId(), saved.getEmail(), saved.getRole().name());
    }

    @Transactional
    public AuthResponse registerWorker(RegisterWorkerRequest request) {
        otpService.assertEmailVerifiedForPurpose(request.email(), OtpPurpose.REGISTER);
        validateUniqueFields(request.email(), request.phone());

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(UserRole.WORKER);
        User savedUser = userRepository.save(user);

        Worker worker = new Worker();
        worker.setUser(savedUser);
        worker.setWorkerType(request.workerType());
        worker.setApprovalStatus(WorkerApprovalStatus.PENDING_REVIEW);
        worker.setKycDocumentUrl(request.kycDocumentUrl());
        worker.setYearsOfExperience(Math.max(request.yearsOfExperience(), 0));
        worker.setLatitude(request.latitude());
        worker.setLongitude(request.longitude());
        worker.setAvailable(false);
        Worker savedWorker = workerRepository.save(worker);
        otpService.consumeVerifiedOtp(request.email(), OtpPurpose.REGISTER);
        auditLogService.record(
                AuditActionType.WORKER_REGISTERED,
                "USER",
                savedUser.getId(),
                "WORKER",
                savedWorker.getId(),
                "Worker registered and pending review in fixcart"
        );

        String token = createToken(savedUser);
        return new AuthResponse(token, savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        requestRateLimitService.assertAllowed("auth:login:" + request.email().toLowerCase(), 10, Duration.ofMinutes(15));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
        );

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        String token = createToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public AuthResponse loginWithOtp(OtpLoginRequest request) {
        otpService.verifyOtp(request.email(), OtpPurpose.LOGIN, request.otpCode());
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadRequestException("User not found with email"));
        otpService.consumeVerifiedOtp(request.email(), OtpPurpose.LOGIN);
        auditLogService.record(AuditActionType.OTP_VERIFIED, "USER", user.getId(), "USER", user.getId(), "OTP login completed in fixcart");
        String token = createToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name());
    }

    private void validateUniqueFields(String email, String phone) {
        if (userRepository.existsByEmail(email.toLowerCase())) {
            throw new BadRequestException("Email already registered");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone already registered");
        }
    }

    private String createToken(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return jwtService.generateToken(userDetails, Map.of("role", user.getRole().name(), "userId", user.getId()));
    }
}
