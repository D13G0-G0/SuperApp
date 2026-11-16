package com.superapp.api.wallet.payment;

import java.math.BigDecimal;

public record PaymentResult(
        PaymentMethod method,
        boolean approved,
        boolean simulated,
        String transactionId,
        BigDecimal fee,
        String message
) {
}