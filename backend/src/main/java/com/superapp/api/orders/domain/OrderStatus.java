package com.superapp.api.orders.domain;

/** Estados de un pedido de compra (FR-06). Las transiciones permitidas están en {@link OrderStateMachine}. */
public enum OrderStatus {
    /** Pagado con la billetera; el dinero queda retenido hasta la entrega. */
    PAID,
    /** El vendedor lo envió. */
    SHIPPED,
    /** El comprador confirmó que lo recibió; el vendedor recibe el dinero. */
    DELIVERED,
    /** Cancelado antes del envío; el comprador recibe un reembolso. */
    CANCELLED
}
