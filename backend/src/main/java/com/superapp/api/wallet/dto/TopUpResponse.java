package com.superapp.api.wallet.dto;

import java.math.BigDecimal;

/** Resultado de una recarga aprobada: el movimiento creado, el saldo nuevo y la comisión simulada. */
public record TopUpResponse(MovementResponse movement, BigDecimal balance, BigDecimal fee, String message) {
}
