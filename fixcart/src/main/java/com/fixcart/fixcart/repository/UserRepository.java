package com.fixcart.fixcart.repository;

import com.fixcart.fixcart.entity.User;
import com.fixcart.fixcart.entity.enums.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    long countByRole(UserRole role);

    List<User> findByRole(UserRole role);
}
