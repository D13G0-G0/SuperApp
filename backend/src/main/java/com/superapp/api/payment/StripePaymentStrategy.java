package com.superapp.api.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class StripePaymentStrategy implements PaymentStrategy {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.029");

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.STRIPE;
    }

    @Override
    public PaymentResult pay(BigDecimal amount) {
        BigDecimal fee = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        return new PaymentResult(
                PaymentMethod.STRIPE,
                true,
                true,
                "SIM-STRIPE-" + UUID.randomUUID(),
                fee,
                "SIMULACIÓN: pago con tarjeta (Stripe) de " + amount.toPlainString()
                        + " COP aprobado. No se procesó dinero real.");
    }
}