package com.stockmaster.backend.config;

import com.stockmaster.backend.security.SessionInactivityFilter;
import com.stockmaster.backend.security.SessionRenewalFilter;
import com.stockmaster.backend.util.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@OpenAPIDefinition
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SessionInactivityFilter sessionInactivityFilter;
    private final SessionRenewalFilter sessionRenewalFilter;


    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SessionInactivityFilter sessionInactivityFilter,
                          SessionRenewalFilter sessionRenewalFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.sessionInactivityFilter = sessionInactivityFilter;
        this.sessionRenewalFilter = sessionRenewalFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Permite acceso público a la documentación de Swagger
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Permite acceso público al login
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**", "/api/reports/**")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/api/movements").hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        // Requiere autenticación para todas las demás rutas
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 1. Filtro de Inactividad (Debe ir primero para detectar la inactividad antes de procesar el JWT)
                .addFilterBefore(sessionInactivityFilter, UsernamePasswordAuthenticationFilter.class)
                // 2. Filtro de Autenticación JWT (Verifica si el token es válido y autentica al usuario)
                .addFilterBefore(jwtAuthenticationFilter, SessionInactivityFilter.class)
                // 3. Filtro de Renovación de Sesión (Se ejecuta después de la autenticación para refrescar el token)
                .addFilterAfter(sessionRenewalFilter, JwtAuthenticationFilter.class);


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Configuración CORS... (Se mantiene igual)

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // ✅ DESPUÉS
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-New-Token"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}