package com.superapp.api.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.superapp.api.wallet.domain.MovementType;
import com.superapp.api.wallet.domain.WalletMovement;

/** Un movimiento tal como lo ve el usuario. signedAmount es negativo cuando sale dinero. */
public record MovementResponse(
        Long id,
        MovementType type,
        BigDecimal amount,
        BigDecimal signedAmount,
        BigDecimal balanceAfter,
        String description,
        String reference,
        LocalDateTime createdAt
) {

    public static MovementResponse from(WalletMovement m) {
        return new MovementResponse(m.getId(), m.getType(), m.getAmount(), m.signedAmount(),
                m.getBalanceAfter(), m.getDescription(), m.getReference(), m.getCreatedAt());
    }
}
