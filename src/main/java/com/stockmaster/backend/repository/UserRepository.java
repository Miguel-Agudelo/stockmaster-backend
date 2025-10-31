package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // consulta para buscar solo usuarios activos
    Optional<User> findByEmailAndIsActive(String email, boolean isActive);
    //contar solo administradores activos
    long countByRoleAndIsActive(String role, boolean isActive);
    // obtener todos los usuarios activos
    List<User> findAllByIsActive(boolean isActive);
}