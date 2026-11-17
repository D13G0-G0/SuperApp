package com.superapp.api.wallet.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Billetera de un usuario (FR-07). Las reglas del dinero viven aquí, sin HTTP ni SQL:
 * <ul>
 * <li>el saldo solo cambia a través de {@link #apply}, que además crea el movimiento;</li>
 * <li>un débito mayor que el saldo se rechaza con {@link InsufficientBalanceException};</li>
 * <li>por eso el saldo siempre es igual a la suma (con signo) de sus movimientos.</li>
 * </ul>
 */
@Entity
@Table(name = "wallet_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Se guarda el id y no la entidad User: el módulo wallet no depende del módulo identity.
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private Long userId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WalletAccount(Long userId) {
        this.userId = userId;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Aplica un movimiento y devuelve el registro inmutable que hay que guardar.
     * El monto siempre es positivo; el tipo decide si suma o resta.
     */
    public WalletMovement apply(MovementType type, BigDecimal amount, String description, String reference) {
        if (type == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio");
        }
        if (amount == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que 0");
        }
        BigDecimal next = type.isCredit() ? balance.add(value) : balance.subtract(value);
        if (next.signum() < 0) {
            throw new InsufficientBalanceException(balance, value);
        }
        this.balance = next;
        return new WalletMovement(this, type, value, next, description, reference);
    }
}
