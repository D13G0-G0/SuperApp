package com.superapp.api.wallet.domain;

import java.math.BigDecimal;

/**
 * Se intenta sacar más dinero del que hay en la billetera.
 * Extiende IllegalArgumentException, así el manejador global responde 400 con el mensaje.
 */
public class InsufficientBalanceException extends IllegalArgumentException {

    private final BigDecimal balance;
    private final BigDecimal requested;

    public InsufficientBalanceException(BigDecimal balance, BigDecimal requested) {
        super("Saldo insuficiente: tienes $" + balance.toPlainString()
                + " y se necesitan $" + requested.toPlainString());
        this.balance = balance;
        this.requested = requested;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getRequested() {
        return requested;
    }
}
