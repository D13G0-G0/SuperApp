package com.superapp.api.shared.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.superapp.api.shared.exception.UnauthorizedException;

/**
 * Implementación TEMPORAL: lee el usuario del header X-User-Id.
 * Se reemplaza por la versión con JWT en el PR de autenticación.
 */
@Component
public class HeaderCurrentUser implements CurrentUser {

    static final String HEADER = "X-User-Id";

    @Override
    public Long id() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String value = attrs == null ? null : attrs.getRequest().getHeader(HEADER);
        if (value == null || value.isBlank()) {
            throw new UnauthorizedException("Debes iniciar sesión");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Sesión inválida");
        }
    }
}
