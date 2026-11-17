package com.superapp.api.wallet.dto;

import java.math.BigDecimal;
import java.util.List;

/** Saldo y movimientos de la billetera (del más reciente al más antiguo). */
public record WalletResponse(BigDecimal balance, List<MovementResponse> movements) {
}
