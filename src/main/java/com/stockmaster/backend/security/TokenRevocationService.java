package com.stockmaster.backend.security;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio que gestiona la Lista Negra (Blacklist) de tokens revocados en memoria.
 * Permite bloquear tokens después de la inactividad o un nuevo inicio de sesión.
 */
@Service
public class TokenRevocationService {

    // CLAVE: JWT (String) | VALOR: Tiempo de Revocación (LocalDateTime)
    private final ConcurrentHashMap<String, LocalDateTime> revokedTokens = new ConcurrentHashMap<>();

    /**
     * Agrega un token a la lista negra.
     * @param token El JWT a revocar.
     */
    public void revokeToken(String token) {
        // Almacenamos el token en la lista negra para bloquear reintentos.
        System.out.println(">>> [DEBUG REVOCATION] Token REVOCADO por Inactividad/Cierre.");
        revokedTokens.put(token, LocalDateTime.now());
    }

    /**
     * Verifica si un token ha sido revocado.
     * @param token El JWT a verificar.
     * @return true si el token está en la lista negra.
     */
    public boolean isRevoked(String token) {
        boolean revoked = revokedTokens.containsKey(token);
        if (revoked) {
            System.out.println(">>> [DEBUG REVOCATION] Token ENCONTRADO como revocado. ACCESO DENEGADO.");
        }
        return revoked;
    }

    /**
     * Elimina un token de la lista negra.
     * NOTA: No es usado directamente en este flujo, pero es útil para mantenimiento.
     * @param token El JWT a limpiar.
     */
    public void unrevokeToken(String token) {
        revokedTokens.remove(token);
    }
}