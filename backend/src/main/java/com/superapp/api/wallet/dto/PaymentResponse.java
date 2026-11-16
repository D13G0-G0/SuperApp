package com.superapp.api.wallet.dto;

import java.math.BigDecimal;

import com.superapp.api.wallet.payment.PaymentMethod;

public record PaymentResponse(
        PaymentMethod method,
        boolean approved,
        boolean simulated,
        String transactionId,
        BigDecimal fee,
        String message
) {
}