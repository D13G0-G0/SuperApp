package com.superapp.api.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class CryptoPaymentStrategy implements PaymentStrategy {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal MINIMUM_AMOUNT = new BigDecimal("10000");

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CRYPTO;
    }

    @Override
    public PaymentResult pay(BigDecimal amount) {
        if (amount.compareTo(MINIMUM_AMOUNT) < 0) {
            return new PaymentResult(
                    PaymentMethod.CRYPTO,
                    false,
                    true,
                    null,
                    BigDecimal.ZERO,
                    "SIMULACIÓN: pago rechazado. El mínimo para criptomonedas es "
                            + MINIMUM_AMOUNT.toPlainString() + " COP.");
        }
        BigDecimal fee = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        return new PaymentResult(
                PaymentMethod.CRYPTO,
                true,
                true,
                "SIM-CRYPTO-" + UUID.randomUUID(),
                fee,
                "SIMULACIÓN: pago con criptomonedas de " + amount.toPlainString()
                        + " COP aprobado. No se procesó dinero real.");
    }
}