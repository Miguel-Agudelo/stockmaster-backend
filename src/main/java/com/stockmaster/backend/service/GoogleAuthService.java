package com.stockmaster.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmaster.backend.entity.User;
import com.stockmaster.backend.repository.UserRepository;
import com.stockmaster.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleAuthService {

    private static final String GOOGLE_TOKEN_INFO_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token=";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${google.client.id}")
    private String googleClientId;

    public GoogleUserInfo verifyGoogleToken(String idToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = GOOGLE_TOKEN_INFO_URL + idToken;
            String response = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);

            String aud = node.has("aud") ? node.get("aud").asText() : "";
            if (!googleClientId.equals(aud)) {
                throw new BadCredentialsException("Token de Google no es válido para esta aplicación.");
            }

            long exp = node.has("exp") ? node.get("exp").asLong() : 0;
            if (exp < System.currentTimeMillis() / 1000) {
                throw new BadCredentialsException("Token de Google expirado.");
            }

            String googleId = node.has("sub") ? node.get("sub").asText() : null;
            String email    = node.has("email") ? node.get("email").asText() : null;
            String name     = node.has("name") ? node.get("name").asText() : null;

            if (googleId == null || email == null) {
                throw new BadCredentialsException("Token de Google inválido: faltan datos esenciales.");
            }

            return new GoogleUserInfo(googleId, email, name);

        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("No se pudo verificar el token de Google: " + e.getMessage());
        }
    }

    public String loginWithGoogle(String idToken) {
        GoogleUserInfo googleUser = verifyGoogleToken(idToken);

        // Buscar usuario por googleId
        User user = userRepository.findByGoogleIdAndIsActive(googleUser.getGoogleId(), true)
                .orElseThrow(() -> new BadCredentialsException(
                        "No existe una cuenta activa vinculada a esta cuenta de Google. " +
                        "Por favor, inicia sesión con tu correo y contraseña y vincula tu cuenta de Google desde tu perfil."));

        return jwtUtil.createToken(user);
    }

    public void linkGoogleAccount(String userEmail, String idToken) {
        GoogleUserInfo googleUser = verifyGoogleToken(idToken);

        if (!userEmail.equalsIgnoreCase(googleUser.getEmail())) {
            throw new IllegalArgumentException(
                    "El correo de la cuenta de Google (" + googleUser.getEmail() +
                    ") no coincide con el correo registrado en el sistema (" + userEmail + "). " +
                    "Debes usar la cuenta de Google asociada a tu correo del sistema.");
        }

        userRepository.findByGoogleId(googleUser.getGoogleId()).ifPresent(existingUser -> {
            if (!existingUser.getEmail().equalsIgnoreCase(userEmail)) {
                throw new IllegalArgumentException(
                        "Esta cuenta de Google ya está vinculada a otro usuario del sistema.");
            }
        });

        User user = userRepository.findByEmailAndIsActive(userEmail, true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado o inactivo."));

        user.setGoogleId(googleUser.getGoogleId());
        userRepository.save(user);
    }

    public void unlinkGoogleAccount(String userEmail) {
        User user = userRepository.findByEmailAndIsActive(userEmail, true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado o inactivo."));

        if (user.getGoogleId() == null) {
            throw new IllegalArgumentException("No tienes ninguna cuenta de Google vinculada.");
        }

        user.setGoogleId(null);
        userRepository.save(user);
    }

    public boolean isGoogleLinked(String userEmail) {
        return userRepository.findByEmailAndIsActive(userEmail, true)
                .map(u -> u.getGoogleId() != null)
                .orElse(false);
    }

    public static class GoogleUserInfo {
        private final String googleId;
        private final String email;
        private final String name;

        public GoogleUserInfo(String googleId, String email, String name) {
            this.googleId = googleId;
            this.email    = email;
            this.name     = name;
        }

        public String getGoogleId() { return googleId; }
        public String getEmail()    { return email; }
        public String getName()     { return name; }
    }
}
