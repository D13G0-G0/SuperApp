package com.superapp.api.dto;

import com.superapp.api.entity.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(
        @NotNull(message = "El usuario es obligatorio")
        Long userId,

        @NotNull(message = "El nuevo estado es obligatorio")
        OrderStatus status
) {
}
