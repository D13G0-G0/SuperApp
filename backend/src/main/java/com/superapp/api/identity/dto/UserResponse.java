package com.superapp.api.identity.dto;

import java.util.Set;

import com.superapp.api.identity.domain.Role;
import com.superapp.api.identity.domain.User;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Set<Role> roles,
        String addressLine,
        String city,
        String state,
        String postalCode,
        String country
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                Set.copyOf(user.getRoles()),
                user.getShippingAddressLine(),
                user.getShippingCity(),
                user.getShippingState(),
                user.getShippingPostalCode(),
                user.getShippingCountry());
    }
}