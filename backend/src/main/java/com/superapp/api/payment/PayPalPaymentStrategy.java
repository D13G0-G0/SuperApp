package com.superapp.api.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class PayPalPaymentStrategy implements PaymentStrategy {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.0349");

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    public PaymentResult pay(BigDecimal amount) {
        BigDecimal fee = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        return new PaymentResult(
                PaymentMethod.PAYPAL,
                true,
                true,
                "SIM-PAYPAL-" + UUID.randomUUID(),
                fee,
                "SIMULACIÓN: pago con PayPal de " + amount.toPlainString()
                        + " COP aprobado. No se procesó dinero real.");
    }
}