package com.superapp.api.identity.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.identity.dto.ShippingAddressRequest;
import com.superapp.api.identity.dto.UserResponse;
import com.superapp.api.identity.domain.User;
import com.superapp.api.shared.exception.ResourceNotFoundException;
import com.superapp.api.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toResponse(findUser(userId));
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
        return UserResponse.from(user);
    }
}