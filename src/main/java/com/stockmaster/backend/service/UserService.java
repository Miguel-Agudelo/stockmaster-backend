package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.UserDto;
import com.stockmaster.backend.dto.UserListDto;
import com.stockmaster.backend.entity.User;
import com.stockmaster.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime; // Importación necesaria para LocalDateTime
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(UserDto userDto) {
        // Validación de contraseña mínima
        final int MIN_PASSWORD_LENGTH = 6;
        if (userDto.getPassword() == null || userDto.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }

        // Se valida contra usuarios activos
        if (userRepository.findByEmailAndIsActive(userDto.getEmail(), true).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }

        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRole(userDto.getRole());
        user.setActive(true);
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByIsActive(true);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));
    }

    public User updateUser(Long id, UserDto userDto) {
        User existingUser = userRepository.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));

        existingUser.setName(userDto.getName());
        existingUser.setEmail(userDto.getEmail());

        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            final int MIN_PASSWORD_LENGTH = 6;
            if (userDto.getPassword().length() < MIN_PASSWORD_LENGTH) {
                throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
            }
            existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        existingUser.setRole(userDto.getRole());
        return userRepository.save(existingUser);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndIsActive(email, true)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = getUserByEmail(email);

        Collection<? extends GrantedAuthority> authorities =
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

    @Transactional
    public void deleteUser(Long id) {
        User userToDelete = userRepository.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));

        // Validación de Administrador Único
        if ("ADMINISTRADOR".equals(userToDelete.getRole())) {
            long adminCount = userRepository.countByRoleAndIsActive("ADMINISTRADOR", true);
            if (adminCount == 1) {
                throw new IllegalStateException("No se puede eliminar al único Administrador del sistema.");
            }
        }

        // 🟢 CORRECCIÓN CLAVE: Asignar la fecha de eliminación
        userToDelete.setDeletedAt(LocalDateTime.now());

        // Borrado lógico
        userToDelete.setActive(false);
        userRepository.save(userToDelete);
    }

    // HU19. Listar usuarios inactivos y mapear a DTO
    public List<UserListDto> getAllInactiveUserDtos() {

        // 1. Obtener las entidades inactivas
        List<User> inactiveUsers = userRepository.findAllByIsActive(false);

        // 2. Mapear cada entidad a su DTO
        return inactiveUsers.stream()
                .map(user -> {
                    UserListDto dto = new UserListDto();
                    dto.setId(user.getId());
                    dto.setName(user.getName());
                    dto.setEmail(user.getEmail());
                    dto.setRole(user.getRole());
                    dto.setCreatedAt(user.getCreatedAt());

                    // 🟢 MAPEO DE DELETEDAT
                    dto.setDeletedAt(user.getDeletedAt());

                    return dto;
                })
                .toList();
    }

    // HU19 - 2. Restaurar Usuario
    @Transactional
    public void restoreUser(Long id) {
        User userToRestore = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));

        if (userToRestore.isActive()) {
            throw new IllegalStateException("El usuario ya se encuentra activo.");
        }

        // 🟢 CORRECCIÓN CLAVE: Limpiar la fecha de eliminación
        userToRestore.setDeletedAt(null);

        userToRestore.setActive(true);
        userRepository.save(userToRestore);
    }
}