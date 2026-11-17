package com.superapp.api.history.api;

/** Tipos del historial, que son también los filtros de la pantalla. */
public enum HistoryType {
    /** Lo que compré y cómo va cada pedido. */
    PURCHASE,
    /** Lo que vendí y cómo va cada venta. */
    SALE,
    /** Recargas de la billetera. */
    WALLET,
    /** Domicilios (módulo de logística). */
    DELIVERY
}
