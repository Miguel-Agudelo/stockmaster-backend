package com.stockmaster.backend.util;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = jwtUtil.getTokenFromRequest(request);

        System.out.println("DEBUG: Token encontrado en request: " + (token != null));

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                // 1. TOKEN VÁLIDO
                System.out.println("DEBUG: ¡TOKEN VÁLIDO! Llenando contexto de seguridad...");
                Claims claims = jwtUtil.getClaims(token);
                String roleName = claims.get("role", String.class);
                Long userId = claims.get("id_user", Long.class);
//Prueba de logs
                String authorityCheck = "ROLE_" + roleName.toUpperCase();
                System.out.println("DEBUG: Authority creado: " + authorityCheck);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userId.toString(),
                                null,
                                // Spring Security espera el prefijo ROLE_
                                List.of(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()))
                        );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                // 2. TOKEN INVÁLIDO (Expirado, firma incorrecta, etc.)
                // Esto ocurre si validateToken devuelve false.
                // Spring Security lo maneja automáticamente, dejando el contexto vacío (no autenticado).
                System.out.println("DEBUG: Token encontrado pero INVALIDO (firma, expiración, etc.).");
            }
        }

        // La cadena de filtros debe continuar, pase o no el token.
        filterChain.doFilter(request, response);
    }
}