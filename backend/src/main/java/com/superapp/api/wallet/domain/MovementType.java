package com.superapp.api.wallet.domain;

/** Tipos de movimiento de la billetera (FR-07). */
public enum MovementType {
    /** Recarga simulada: entra dinero. */
    TOP_UP(true),
    /** Pago de una compra: sale dinero. */
    PAYMENT(false),
    /** Ingreso por una venta: entra dinero. */
    INCOME(true),
    /** Reembolso de un pedido cancelado: entra dinero. */
    REFUND(true);

    private final boolean credit;

    MovementType(boolean credit) {
        this.credit = credit;
    }

    /** true si el movimiento suma al saldo; false si resta. */
    public boolean isCredit() {
        return credit;
    }
}
