package com.superapp.api.logistics.dto;

import com.superapp.api.logistics.domain.DeliveryStatus;

import jakarta.validation.constraints.NotNull;

public record DeliveryStatusRequest(
        @NotNull(message = "El usuario es obligatorio")
        Long userId,

        @NotNull(message = "El nuevo estado es obligatorio")
        DeliveryStatus status
) {
}
