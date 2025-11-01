package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.UserDto;
import com.stockmaster.backend.entity.User;
import com.stockmaster.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // 💡 Implementación de la interfaz
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class UserService implements UserDetailsService { // 🟢 Implementación

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
            // Se puede añadir una validación aquí también si el usuario cambia la contraseña en modo edición
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

    /**
     * 🟢 Implementación del método UserDetailsService.
     * Es requerido por Spring Security para cargar los detalles del usuario
     * basándose en el nombre de usuario (email en este caso).
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Busca el usuario en la DB
        User user = getUserByEmail(email);

        // 2. Define las autoridades/roles (usando el prefijo ROLE_ para Spring Security)
        Collection<? extends GrantedAuthority> authorities =
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));

        // 3. Retorna un objeto UserDetails de Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(), // La contraseña ya viene encriptada de la DB
                authorities
        );
    }

    @Transactional
    public void deleteUser(Long id) {
        User userToDelete = userRepository.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));
        // Se valida contra usuarios activos
        if ("ADMINISTRADOR".equals(userToDelete.getRole())) {
            long adminCount = userRepository.countByRoleAndIsActive("ADMINISTRADOR", true);
            if (adminCount == 1) {
                throw new IllegalStateException("No se puede eliminar al único Administrador del sistema.");
            }
        }
        // Borrado lógico: se marca como inactivo
        userToDelete.setActive(false);
        userRepository.save(userToDelete);
    }

    // HU19. Listar usuarios inactivos
    public List<User> getAllInactiveUsers() {
        return userRepository.findAllByIsActive(false);
    }

    // HU19 - 2. Restaurar Usuario
    @Transactional
    public void restoreUser(Long id) {
        User userToRestore = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));

        if (userToRestore.isActive()) {
            throw new IllegalStateException("El usuario ya se encuentra activo.");
        }

        userToRestore.setActive(true);
        userRepository.save(userToRestore);
    }
}