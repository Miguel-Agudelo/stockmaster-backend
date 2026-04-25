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
                System.out.println("DEBUG: ¡TOKEN VÁLIDO! Llenando contexto de seguridad...");
                Claims claims = jwtUtil.getClaims(token);
                String roleName = claims.get("role", String.class);

                // ✅ CORRECCIÓN: usamos el email (subject) como principal,
                // no el ID. Así authentication.getName() devuelve el email
                // y getUserByEmail() puede encontrar al usuario correctamente.
                String email = claims.getSubject();

                String authorityCheck = "ROLE_" + roleName.toUpperCase();
                System.out.println("DEBUG: Authority creado: " + authorityCheck);
                System.out.println("DEBUG: Principal (email): " + email);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                email,   // ← email, no userId.toString()
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()))
                        );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("DEBUG: Token encontrado pero INVALIDO (firma, expiración, etc.).");
            }
        }

        filterChain.doFilter(request, response);
    }
}