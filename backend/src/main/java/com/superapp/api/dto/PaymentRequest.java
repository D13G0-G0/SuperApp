package com.superapp.api.dto;

import java.math.BigDecimal;

import com.superapp.api.payment.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull(message = "El método de pago es obligatorio")
        PaymentMethod method,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que 0")
        BigDecimal amount
) {
}