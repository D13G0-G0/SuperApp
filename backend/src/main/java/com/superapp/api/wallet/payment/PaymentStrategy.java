package com.superapp.api.wallet.payment;

import java.math.BigDecimal;

public interface PaymentStrategy {

    PaymentMethod getMethod();

    PaymentResult pay(BigDecimal amount);
}