package com.fixcart.fixcart.config;

import com.fixcart.fixcart.entity.User;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${fixcart.admin.bootstrap.enabled:false}")
    private boolean adminBootstrapEnabled;

    @Value("${fixcart.admin.bootstrap.email:admin@fixcart.com}")
    private String adminEmail;

    @Value("${fixcart.admin.bootstrap.password:admin123}")
    private String adminPassword;

    @Value("${fixcart.admin.bootstrap.phone:9000000000}")
    private String adminPhone;

    @Value("${fixcart.admin.bootstrap.full-name:Fixcart Admin}")
    private String adminFullName;

    @Bean
    public CommandLineRunner fixcartAdminBootstrapRunner() {
        return args -> {
            if (!adminBootstrapEnabled) {
                return;
            }
            if (userRepository.existsByEmail(adminEmail.toLowerCase())) {
                return;
            }
            User admin = new User();
            admin.setFullName(adminFullName);
            admin.setEmail(adminEmail.toLowerCase());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setPhone(adminPhone);
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
            log.info("fixcart admin bootstrap user created email={}", adminEmail);
        };
    }
}
