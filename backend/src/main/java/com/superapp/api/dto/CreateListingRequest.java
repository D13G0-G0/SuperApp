package com.superapp.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateListingRequest(
        @NotNull(message = "El vendedor es obligatorio")
        Long sellerId,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String title,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
        BigDecimal price,

        @NotBlank(message = "La categoría es obligatoria")
        String category,

        @NotBlank(message = "Debes incluir al menos una foto")
        @Size(max = 500, message = "El enlace de la foto es demasiado largo")
        String imageUrl
) {
}