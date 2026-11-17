package com.superapp.api.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.superapp.api.shared.exception.UnauthorizedException;

/** El usuario actual sale del JWT ya validado: el "sub" del token es su id. */
@Component
public class JwtCurrentUser implements CurrentUser {

    @Override
    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            try {
                return Long.valueOf(jwt.getToken().getSubject());
            } catch (NumberFormatException e) {
                throw new UnauthorizedException("Sesión inválida");
            }
        }
        throw new UnauthorizedException("Debes iniciar sesión");
    }
}
