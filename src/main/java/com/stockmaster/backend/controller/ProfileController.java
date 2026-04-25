package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.ChangePasswordDto;
import com.stockmaster.backend.entity.User;
import com.stockmaster.backend.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@SecurityRequirement(name = "BearerAuth")
public class ProfileController {

    @Autowired
    private UserService userService;

    /**
     * GET /api/profile/me
     * Retorna los datos del usuario autenticado (nombre, email, rol).
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<?> getMyProfile() {
        try {
            String email = getEmailFromContext();
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "No se pudo identificar al usuario autenticado."));
            }

            User user = userService.getUserByEmail(email);
            Map<String, Object> profile = Map.of(
                    "id",    user.getId(),
                    "name",  user.getName(),
                    "email", user.getEmail(),
                    "role",  user.getRole()
            );
            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado."));
        }
    }

    /**
     * PUT /api/profile/change-password
     * Cambia la contraseña del usuario autenticado verificando la contraseña actual.
     */
    @PutMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDto dto) {
        try {
            String email = getEmailFromContext();
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "No se pudo identificar al usuario autenticado."));
            }

            userService.changePassword(email, dto);
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno. Intente de nuevo más tarde."));
        }
    }

    /**
     * Lee el email (username) del usuario desde el SecurityContext.
     * Más fiable que @AuthenticationPrincipal porque no depende de inyección de parámetros.
     */
    private String getEmailFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}