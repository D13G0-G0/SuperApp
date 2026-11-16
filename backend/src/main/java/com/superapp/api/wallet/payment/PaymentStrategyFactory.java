package com.superapp.api.wallet.payment;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class PaymentStrategyFactory {

    private final Map<PaymentMethod, PaymentStrategy> strategies = new EnumMap<>(PaymentMethod.class);

    public PaymentStrategyFactory(List<PaymentStrategy> availableStrategies) {
        for (PaymentStrategy strategy : availableStrategies) {
            strategies.put(strategy.getMethod(), strategy);
        }
    }

    public PaymentStrategy get(PaymentMethod method) {
        PaymentStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("Método de pago no soportado: " + method);
        }
        return strategy;
    }
}