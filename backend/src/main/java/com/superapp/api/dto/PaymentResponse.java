package com.superapp.api.dto;

import java.math.BigDecimal;

import com.superapp.api.payment.PaymentMethod;

public record PaymentResponse(
        PaymentMethod method,
        boolean approved,
        boolean simulated,
        String transactionId,
        BigDecimal fee,
        String message
) {
}