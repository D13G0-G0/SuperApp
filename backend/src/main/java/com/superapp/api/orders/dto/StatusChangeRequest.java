package com.superapp.api.orders.dto;

import com.superapp.api.orders.domain.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(@NotNull(message = "Falta el estado nuevo") OrderStatus status) {
}
