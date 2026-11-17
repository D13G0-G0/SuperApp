package com.superapp.api.wallet.dto;

import java.math.BigDecimal;

import com.superapp.api.wallet.payment.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Recarga simulada de la billetera. */
public record TopUpRequest(
        @NotNull(message = "Elige un método de pago")
        PaymentMethod method,

        @NotNull(message = "Escribe cuánto quieres recargar")
        @DecimalMin(value = "1000", message = "La recarga mínima es de $1.000")
        @Digits(integer = 12, fraction = 2, message = "El monto no es válido")
        BigDecimal amount
) {
}
