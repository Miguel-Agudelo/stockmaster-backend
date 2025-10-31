package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndIsActive(String email, boolean isActive);
    long countByRoleAndIsActive(String role, boolean isActive);
    List<User> findAllByIsActive(boolean isActive);
}