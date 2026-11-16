package com.superapp.api.entity;

public enum OrderStatus {
    PENDING,     // esperando que un conductor lo acepte
    ACCEPTED,    // un conductor lo aceptó y va a recogerlo
    PICKED_UP,   // el conductor ya recogió el pedido y va en camino
    DELIVERED,   // entregado al cliente
    CANCELLED    // cancelado por el cliente antes de ser aceptado
}
