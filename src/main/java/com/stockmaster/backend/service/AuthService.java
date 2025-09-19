package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.LoginDto;
import com.stockmaster.backend.entity.User;
import com.stockmaster.backend.repository.UserRepository;
import com.stockmaster.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public String authenticate(LoginDto request) {
        if (request.getEmail() == null || request.getPassword() == null || request.getEmail().isEmpty() || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Email y contraseña no pueden estar vacíos.");
        }
        // Se busca el usuario por email y se verifica que esté activo
        User user = userRepository.findByEmailAndIsActive(request.getEmail(), true)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas. Verifique su email y contraseña."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales incorrectas. Verifique su email y contraseña.");
        }
        // Si las credenciales son correctas y el usuario está activo, se crea el token
        return jwtUtil.createToken(user);
    }
}
