package com.fixcart.fixcart.service;

import com.fixcart.fixcart.entity.User;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.exception.ResourceNotFoundException;
import com.fixcart.fixcart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Long extractUserId(String email) {
        return findByEmail(email).getId();
    }

    public UserRole extractRole(String email) {
        return findByEmail(email).getRole();
    }
}
