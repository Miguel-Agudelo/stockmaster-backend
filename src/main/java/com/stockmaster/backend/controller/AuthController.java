package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.LoginDto;
import com.stockmaster.backend.entity.User;
import com.stockmaster.backend.service.AuthService;
import com.stockmaster.backend.service.UserService;
import com.stockmaster.backend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;

    // ── Login ─────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginDto loginDto) {
        try {
            String token = authService.authenticate(loginDto);

            User user = userService.getUserByEmail(loginDto.getEmail());
            if (user == null) {
                throw new BadCredentialsException("Usuario no encontrado o inactivo.");
            }

            Claims claims = jwtUtil.getClaims(token);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Login exitoso");

            Map<String, Object> userData = new HashMap<>();
            userData.put("id_user", claims.get("id_user", Long.class));
            userData.put("role",    claims.get("role",    String.class));
            userData.put("name",    user.getName());
            response.put("user", userData);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales incorrectas. Verifique su email y contraseña."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor. Intente de nuevo más tarde."));
        }
    }

    // ── Ping — mantener sesión activa ─────────────────────────────────────────
    /**
     * Endpoint ligero que el frontend llama periódicamente cuando detecta actividad
     * del usuario. Tiene dos efectos en el backend:
     *   1. SessionInactivityFilter registra la petición y reinicia el timer de inactividad.
     *   2. SessionRenewalFilter detecta si el JWT está próximo a expirar y,
     *      si es así, devuelve un nuevo token en la cabecera X-New-Token.
     *
     * El interceptor de api.js en el frontend ya captura X-New-Token automáticamente.
     */
    @GetMapping("/ping")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
