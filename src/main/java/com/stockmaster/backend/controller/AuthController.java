package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.LoginDto;
import com.stockmaster.backend.entity.User;
import com.stockmaster.backend.repository.UserRepository;
import com.stockmaster.backend.service.AuthService;
import com.stockmaster.backend.service.UserService;
import com.stockmaster.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil; // Inyecta JwtUtil para decodificar el token

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginDto loginDto) {
        try {
            String token = authService.authenticate(loginDto); // Obtiene el token

            User user = userService.getUserByEmail(loginDto.getEmail());
            if (user == null) {
                throw new BadCredentialsException("Usuario no encontrado o inactivo.");
            }
            // Decodifica los claims del token
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Login exitoso");

            // Extrae id_user y role desde los claims del token
            Map<String, Object> userData = new HashMap<>();
            userData.put("id_user", jwtUtil.getClaims(token).get("id_user", Long.class));
            userData.put("role", jwtUtil.getClaims(token).get("role", String.class));
            response.put("user", userData);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Credenciales incorrectas. Verifique su email y contraseña.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor. Intente de nuevo más tarde.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}