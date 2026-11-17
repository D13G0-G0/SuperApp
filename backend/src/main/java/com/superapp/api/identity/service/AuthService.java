package com.superapp.api.identity.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.identity.domain.Role;
import com.superapp.api.identity.domain.User;
import com.superapp.api.identity.dto.AuthResponse;
import com.superapp.api.identity.dto.LoginRequest;
import com.superapp.api.identity.dto.RegisterRequest;
import com.superapp.api.identity.dto.UserResponse;
import com.superapp.api.identity.repository.UserRepository;
import com.superapp.api.shared.exception.DuplicateResourceException;
import com.superapp.api.shared.exception.UnauthorizedException;
import com.superapp.api.shared.security.TokenService;

import lombok.RequiredArgsConstructor;

/** Registro e inicio de sesión (FR-01, FR-02). */
@Service
@RequiredArgsConstructor
public class AuthService {

    // Mismo mensaje para correo inexistente y contraseña mala: no revela qué correos existen
    static final String BAD_CREDENTIALS = "Correo o contraseña incorrectos";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.roles().contains(Role.ADMIN)) {
            throw new IllegalArgumentException("El rol ADMIN no se puede elegir al registrarse");
        }
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Ya existe una cuenta con ese correo");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.getRoles().addAll(request.roles());
        return session(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalize(request.email()))
                .orElseThrow(() -> new UnauthorizedException(BAD_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        return session(user);
    }

    private AuthResponse session(User user) {
        var roles = user.getRoles().stream().map(Role::name).sorted().toList();
        var token = tokenService.issue(user.getId(), user.getEmail(), roles);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), UserResponse.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
