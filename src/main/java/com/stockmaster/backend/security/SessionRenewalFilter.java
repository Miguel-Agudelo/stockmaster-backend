package com.stockmaster.backend.security;

import com.stockmaster.backend.entity.User;
import com.stockmaster.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que renueva el token si el usuario está activo (hace peticiones)
 * y el token está cerca de expirar.
 */
@Component
public class SessionRenewalFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SessionRenewalFilter.class);

    private final JwtUtil jwtUtil;

    // Umbral de Renovación: 60 segundos (60,000 ms). Si quedan menos de 60s, renovar.
    private static final long RENEWAL_THRESHOLD_MS = 60000L;

    public SessionRenewalFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Solo procesar si el usuario ya fue autenticado por JwtAuthenticationFilter
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {

            String jwt = jwtUtil.getTokenFromRequest(request);

            if (jwt != null) {

                long remainingTimeMs = jwtUtil.getRemainingTimeInMs(jwt);

                // Verificar si el token es válido y está dentro del umbral de renovación
                // 1ms < remainingTimeMs < 60000ms
                if (remainingTimeMs > 0 && remainingTimeMs < RENEWAL_THRESHOLD_MS) {

                    // ⚠ Importante: Asumimos que el principal en Spring Security contiene la entidad User,
                    // lo cual es una práctica común, o al menos tiene la información necesaria para crearla.
                    // Si tu UserDetailsService devuelve un UserDetails de Spring, adapta esto.
                    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

                    // Intentamos obtener el usuario que inició sesión
                    if (principal instanceof User) {

                        User user = (User) principal;

                        // Generar el nuevo token
                        String newJwt = jwtUtil.createToken(user);

                        // Devolver el nuevo token en un encabezado CUSTOM (X-New-Token).
                        response.setHeader("X-New-Token", newJwt);
                        logger.info("Token JWT renovado automáticamente para el usuario: " + user.getEmail());
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}