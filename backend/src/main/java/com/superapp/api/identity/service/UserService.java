package com.superapp.api.identity.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.identity.dto.CreateUserRequest;
import com.superapp.api.identity.dto.ShippingAddressRequest;
import com.superapp.api.identity.dto.UserResponse;
import com.superapp.api.identity.domain.Role;
import com.superapp.api.identity.domain.User;
import com.superapp.api.shared.exception.DuplicateResourceException;
import com.superapp.api.shared.exception.ResourceNotFoundException;
import com.superapp.api.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (request.roles().contains(Role.ADMIN)) {
            throw new IllegalArgumentException("El rol ADMIN no se puede elegir al registrarse");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Ya existe un usuario con ese correo");
        }
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.getRoles().addAll(request.roles());
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toResponse(findUser(userId));
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una cuenta con ese correo"));
    }

    @Transactional
    public UserResponse updateShippingAddress(Long userId, ShippingAddressRequest request) {
        User user = findUser(userId);

        // PATCH: solo se modifican los campos que llegan en la petición
        if (request.addressLine() != null) {
            user.setShippingAddressLine(request.addressLine());
        }
        if (request.city() != null) {
            user.setShippingCity(request.city());
        }
        if (request.state() != null) {
            user.setShippingState(request.state());
        }
        if (request.postalCode() != null) {
            user.setShippingPostalCode(request.postalCode());
        }
        if (request.country() != null) {
            user.setShippingCountry(request.country());
        }

        return toResponse(userRepository.save(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles(),
                user.getShippingAddressLine(),
                user.getShippingCity(),
                user.getShippingState(),
                user.getShippingPostalCode(),
                user.getShippingCountry());
    }
}