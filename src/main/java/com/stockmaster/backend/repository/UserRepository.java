package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndIsActive(String email, boolean isActive);
    long countByRoleAndIsActive(String role, boolean isActive);
    List<User> findAllByIsActive(boolean isActive);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByGoogleIdAndIsActive(String googleId, boolean isActive);
}
