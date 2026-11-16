package com.superapp.api.payment;

import java.math.BigDecimal;

public interface PaymentStrategy {

    PaymentMethod getMethod();

    PaymentResult pay(BigDecimal amount);
}