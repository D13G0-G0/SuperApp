package com.superapp.api.dto;

import java.util.Set;

import com.superapp.api.entity.Role;

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
}