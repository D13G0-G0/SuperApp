package com.superapp.api.wallet.api;

import java.math.BigDecimal;

/**
 * Interfaz pública del módulo wallet: es lo ÚNICO que otros módulos (orders, history...) pueden usar
 * para mover dinero. Nunca deben tocar los repositorios ni las entidades de wallet.
 *
 * <p>Los métodos se unen a la transacción de quien los llama: si el checkout falla después de un
 * débito, el débito también se deshace (NFR-02).</p>
 */
public interface IWallet {

    /** Saldo actual del usuario (0 si todavía no tiene billetera). */
    BigDecimal balance(Long userId);

    /**
     * Saca dinero de la billetera (por ejemplo, el pago de una compra).
     * Bloquea la fila de la billetera hasta el final de la transacción.
     *
     * @throws com.superapp.api.wallet.domain.InsufficientBalanceException si no alcanza el saldo
     */
    MovementReceipt debit(Long userId, BigDecimal amount, String description, String reference);

    /** Mete dinero en la billetera: ingreso por una venta (INCOME) o reembolso (REFUND). */
    MovementReceipt credit(Long userId, CreditType type, BigDecimal amount, String description, String reference);

    /** Tipos de crédito que otros módulos pueden registrar (las recargas solo las hace wallet). */
    enum CreditType {
        INCOME,
        REFUND
    }

    /** Comprobante del movimiento que quedó registrado. */
    record MovementReceipt(Long movementId, BigDecimal amount, BigDecimal balanceAfter) {
    }
}
