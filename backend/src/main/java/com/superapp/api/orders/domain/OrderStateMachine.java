package com.superapp.api.orders.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados del pedido (FR-06): qué transiciones existen y quién puede hacer cada una.
 *
 * <pre>
 * PAID ──(vendedor)──▶ SHIPPED ──(comprador)──▶ DELIVERED
 *   └──(comprador o vendedor)──▶ CANCELLED
 * </pre>
 *
 * Cualquier otra transición se rechaza y el mensaje dice cuáles sí se permiten.
 * Es código puro (sin Spring ni base de datos) para poder probarlo solo.
 */
public final class OrderStateMachine {

    /** Quién pide el cambio, respecto al pedido. */
    public enum Party {
        BUYER,
        SELLER
    }

    private record Rule(OrderStatus to, Set<Party> allowed) {
    }

    private static final Map<OrderStatus, List<Rule>> RULES = new EnumMap<>(OrderStatus.class);

    static {
        RULES.put(OrderStatus.PAID, List.of(
                new Rule(OrderStatus.SHIPPED, Set.of(Party.SELLER)),
                new Rule(OrderStatus.CANCELLED, Set.of(Party.BUYER, Party.SELLER))));
        RULES.put(OrderStatus.SHIPPED, List.of(
                new Rule(OrderStatus.DELIVERED, Set.of(Party.BUYER))));
        RULES.put(OrderStatus.DELIVERED, List.of());
        RULES.put(OrderStatus.CANCELLED, List.of());
    }

    private OrderStateMachine() {
    }

    /** Estados a los que esta parte puede llevar el pedido desde el estado actual. */
    public static List<OrderStatus> allowed(OrderStatus from, Party party) {
        return RULES.get(from).stream().filter(r -> r.allowed().contains(party)).map(Rule::to).toList();
    }

    /**
     * Revisa que la transición exista y que la parte pueda hacerla.
     *
     * @throws InvalidTransitionException si no está permitida
     */
    public static void check(OrderStatus from, OrderStatus to, Party party) {
        if (to == null) {
            throw new IllegalArgumentException("El estado nuevo es obligatorio");
        }
        if (!allowed(from, party).contains(to)) {
            throw new InvalidTransitionException(from, to, allowed(from, party));
        }
    }
}
