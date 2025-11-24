package com.stockmaster.backend.util;

import com.stockmaster.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET_KEY_STRING = "StockMasterClaveSecretaUnica32Byte";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    // 120000 ms = 2 minutos (Tiempo de vida del token)
    private final long validityInMilliseconds = 1800000;

    public String createToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("id_user", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validityInMilliseconds))
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

    public String getUsernameFromJWT(String token) {
        return getClaims(token).getSubject();
    }

    public long getRemainingTimeInMs(String token) {
        try {
            Claims claims = getClaims(token);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - new Date().getTime();
            return Math.max(0, remaining);
        } catch (ExpiredJwtException ex) {
            // Si ya expiró, el tiempo restante es 0
            return 0;
        } catch (Exception ex) {
            // Error general
            return -1;
        }
    }

    public boolean validateToken(String token) {
        try {
            // El token es válido si está firmado correctamente y no ha expirado
            getClaims(token); // Si no lanza excepción, es válido.
            return true;
        } catch (Exception e) {
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