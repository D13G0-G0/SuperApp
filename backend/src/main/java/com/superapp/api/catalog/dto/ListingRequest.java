package com.superapp.api.catalog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para crear o editar una publicación (US-01). Cada campo inválido trae su propio mensaje. */
public record ListingRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String title,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
        @Digits(integer = 10, fraction = 2, message = "El precio no es válido")
        BigDecimal price,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 60, message = "La categoría no puede superar 60 caracteres")
        String category,

        @NotBlank(message = "Debes incluir una foto")
        @Size(max = 500, message = "El enlace de la foto es demasiado largo")
        @Pattern(regexp = "^https?://\\S+$", message = "La foto debe ser un enlace que empiece por http:// o https://")
        String imageUrl
) {
}
