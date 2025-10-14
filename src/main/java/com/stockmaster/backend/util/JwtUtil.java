package com.stockmaster.backend.util;

import com.stockmaster.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey; // Asegúrate de usar javax.crypto.SecretKey o java.security.Key
import java.util.Date;

@Component
public class JwtUtil {

    // 🟢 CLAVE CORREGIDA: Usa 32 caracteres fijos.
    private static final String SECRET_KEY_STRING = "StockMasterClaveSecretaUnica32Byte";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    // 1800000 ms = 30 minutos
    private final long validityInMilliseconds = 1800000;

    public String createToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("id_user", user.getId())
                // 💡 El rol se inyecta aquí como 'ADMINISTRADOR' u 'OPERADOR'
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validityInMilliseconds))
                // Asegúrate de que las importaciones coincidan con el uso de 'SECRET_KEY'
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            // El token es válido si está firmado correctamente y no ha expirado
            return !getClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            // Esto atrapa tokens inválidos, expirados o con firma incorrecta
            return false;
        }
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}