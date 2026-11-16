package com.superapp.api.identity.dto;

import java.util.Set;

import com.superapp.api.identity.domain.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String fullName,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        String email,

        @NotEmpty(message = "Debes elegir al menos un rol")
        Set<Role> roles
) {
}