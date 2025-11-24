package com.stockmaster.backend.security;

import com.stockmaster.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de Inactividad de Sesión. Marca el token como revocado si no hay actividad
 * por un periodo.
 */
@Component
public class SessionInactivityFilter extends OncePerRequestFilter {

    private final TokenRevocationService tokenRevocationService;
    private final JwtUtil jwtUtil; // Inyectamos JwtUtil para extraer el token

    // Mapa para rastrear el último acceso (clave: token, valor: timestamp)
    private final ConcurrentHashMap<String, LocalDateTime> lastAccessMap = new ConcurrentHashMap<>();

    // Tiempo de inactividad, debe coincidir con el tiempo del frontend (110 segundos = 1.83 minutos)
    @Value("${session.inactivity.minutes:2}") // Usamos 2 minutos por defecto si no está en application.properties
    private long inactivityMinutes;

    // Ruta de autenticación para excluir
    private static final String AUTH_PATH = "/api/auth/login";
    // La ruta de refresh (si existiera) también debe excluirse, pero la eliminamos.

    public SessionInactivityFilter(TokenRevocationService tokenRevocationService, JwtUtil jwtUtil) {
        this.tokenRevocationService = tokenRevocationService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. VERIFICACIÓN DE EXCLUSIÓN (Saltar el filtro si es la petición de login)
        if (request.getServletPath().equals(AUTH_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtUtil.getTokenFromRequest(request);

        if (token != null && !token.isEmpty()) {

            // 2. VERIFICACIÓN DE REVOCACIÓN (Si ya está en lista negra, bloquear)
            if (tokenRevocationService.isRevoked(token)) {
                sendExpiredResponse(response, "Su sesión ha sido cerrada por inactividad o un nuevo inicio de sesión.");
                return;
            }

            // 3. VERIFICACIÓN DE INACTIVIDAD POR TIEMPO
            LocalDateTime lastAccess = lastAccessMap.get(token);
            LocalDateTime now = LocalDateTime.now();
            boolean isInactive = false;

            if (lastAccess != null) {
                Duration duration = Duration.between(lastAccess, now);

                if (duration.toMinutes() >= inactivityMinutes) {
                    isInactive = true;
                }
            }

            if (isInactive) {
                // Sesión expirada por inactividad
                lastAccessMap.remove(token);
                // CLAVE: Lo agregamos a la lista negra para bloquear cualquier reintento
                tokenRevocationService.revokeToken(token);
                sendExpiredResponse(response, "Su sesión ha expirado por inactividad.");
                return; // Detener la cadena
            }

            // 4. Mantener Sesión Activa (Actualizar marca de tiempo).
            lastAccessMap.put(token, now);
        }

        filterChain.doFilter(request, response);
    }

    // --- Métodos Auxiliares ---
    private void sendExpiredResponse(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}