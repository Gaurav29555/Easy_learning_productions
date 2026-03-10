package com.fixcart.fixcart.service;

import com.fixcart.fixcart.dto.AuthResponse;
import com.fixcart.fixcart.dto.LoginRequest;
import com.fixcart.fixcart.dto.OtpLoginRequest;
import com.fixcart.fixcart.dto.RegisterUserRequest;
import com.fixcart.fixcart.dto.RegisterWorkerRequest;
import com.fixcart.fixcart.entity.User;
import com.fixcart.fixcart.entity.Worker;
import com.fixcart.fixcart.entity.enums.OtpPurpose;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.exception.BadRequestException;
import com.fixcart.fixcart.repository.UserRepository;
import com.fixcart.fixcart.repository.WorkerRepository;
import com.fixcart.fixcart.security.JwtService;
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

    @Transactional
    public AuthResponse registerCustomer(RegisterUserRequest request) {
        otpService.assertPhoneVerifiedForPurpose(request.phone(), OtpPurpose.REGISTER);
        validateUniqueFields(request.email(), request.phone());

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(UserRole.CUSTOMER);
        User saved = userRepository.save(user);
        otpService.consumeVerifiedOtp(request.phone(), OtpPurpose.REGISTER);

        String token = createToken(saved);
        return new AuthResponse(token, saved.getId(), saved.getEmail(), saved.getRole().name());
    }

    @Transactional
    public AuthResponse registerWorker(RegisterWorkerRequest request) {
        otpService.assertPhoneVerifiedForPurpose(request.phone(), OtpPurpose.REGISTER);
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
        worker.setLatitude(request.latitude());
        worker.setLongitude(request.longitude());
        worker.setAvailable(true);
        workerRepository.save(worker);
        otpService.consumeVerifiedOtp(request.phone(), OtpPurpose.REGISTER);

        String token = createToken(savedUser);
        return new AuthResponse(token, savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
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
        otpService.verifyOtp(request.phone(), OtpPurpose.LOGIN, request.otpCode());
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new BadRequestException("User not found with phone"));
        otpService.consumeVerifiedOtp(request.phone(), OtpPurpose.LOGIN);
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
