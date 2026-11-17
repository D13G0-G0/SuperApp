package com.superapp.api.orders.domain;

import java.util.List;
import java.util.stream.Collectors;

/** Transición de estado no permitida. Responde 400 y dice qué cambios sí se pueden hacer. */
public class InvalidTransitionException extends IllegalArgumentException {

    private final List<OrderStatus> allowed;

    public InvalidTransitionException(OrderStatus from, OrderStatus to, List<OrderStatus> allowed) {
        super("No se puede pasar el pedido de " + from + " a " + to + ". "
                + (allowed.isEmpty()
                        ? "Desde " + from + " no puedes hacer más cambios."
                        : "Desde " + from + " puedes pasar a: "
                                + allowed.stream().map(Enum::name).collect(Collectors.joining(", ")) + "."));
        this.allowed = allowed;
    }

    public List<OrderStatus> getAllowed() {
        return allowed;
    }
}
