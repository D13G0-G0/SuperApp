package com.superapp.api.history.api;

import java.math.BigDecimal;

/**
 * Lo único que los otros módulos ven del historial (FR-08): anotar algo que le pasó a un usuario.
 *
 * Se debe llamar dentro de la transacción de la operación (propagación MANDATORY): así el registro
 * se guarda solo si la operación se guarda, y nunca queda un "compraste" de una compra que se deshizo.
 */
public interface IHistory {

    /**
     * @param amount    dinero que entró (+) o salió (−) de la billetera del usuario por este hecho;
     *                  {@code null} si no movió dinero
     * @param reference referencia para encontrarlo (p. ej. "ORDER-12"); puede ser {@code null}
     */
    void record(Long userId, HistoryType type, String title, String detail, BigDecimal amount, String reference);
}
