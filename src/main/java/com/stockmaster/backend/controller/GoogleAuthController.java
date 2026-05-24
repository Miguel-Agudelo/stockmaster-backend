package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.GoogleLoginDto;
import com.stockmaster.backend.entity.User;
import com.stockmaster.backend.service.GoogleAuthService;
import com.stockmaster.backend.service.UserService;
import com.stockmaster.backend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> loginWithGoogle(@RequestBody GoogleLoginDto dto) {
        try {
            if (dto.getIdToken() == null || dto.getIdToken().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "El token de Google es requerido."));
            }

            String token = googleAuthService.loginWithGoogle(dto.getIdToken());
            Claims claims = jwtUtil.getClaims(token);

            String email = claims.getSubject();
            User user = userService.getUserByEmail(email);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Login con Google exitoso");

            Map<String, Object> userData = new HashMap<>();
            userData.put("id_user", claims.get("id_user", Long.class));
            userData.put("role",    claims.get("role",    String.class));
            userData.put("name",    user.getName());
            response.put("user", userData);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor. Intente de nuevo más tarde."));
        }
    }

    @PostMapping("/link")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<?> linkGoogleAccount(@RequestBody GoogleLoginDto dto) {
        try {
            String userEmail = getEmailFromContext();
            if (userEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "No se pudo identificar al usuario autenticado."));
            }

            if (dto.getIdToken() == null || dto.getIdToken().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "El token de Google es requerido."));
            }

            googleAuthService.linkGoogleAccount(userEmail, dto.getIdToken());
            return ResponseEntity.ok(Map.of("message", "Cuenta de Google vinculada exitosamente."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor. Intente de nuevo más tarde."));
        }
    }

    @DeleteMapping("/unlink")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<?> unlinkGoogleAccount() {
        try {
            String userEmail = getEmailFromContext();
            if (userEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "No se pudo identificar al usuario autenticado."));
            }

            googleAuthService.unlinkGoogleAccount(userEmail);
            return ResponseEntity.ok(Map.of("message", "Cuenta de Google desvinculada exitosamente."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor. Intente de nuevo más tarde."));
        }
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<?> getGoogleLinkStatus() {
        try {
            String userEmail = getEmailFromContext();
            if (userEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "No se pudo identificar al usuario autenticado."));
            }

            boolean linked = googleAuthService.isGoogleLinked(userEmail);
            return ResponseEntity.ok(Map.of("googleLinked", linked));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor."));
        }
    }

    private String getEmailFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
