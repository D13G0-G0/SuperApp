package com.superapp.api.identity.dto;

import java.time.Instant;

/** Respuesta de registro e inicio de sesión: el token y los datos del usuario. */
public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UserResponse user
) {
}
