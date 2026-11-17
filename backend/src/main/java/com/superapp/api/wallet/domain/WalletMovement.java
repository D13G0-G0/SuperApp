package com.superapp.api.wallet.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un movimiento de la billetera (FR-07). Es inmutable: no tiene setters y Hibernate nunca lo
 * actualiza (@Immutable). Solo lo crea {@link WalletAccount#apply} al sumar o restar dinero.
 */
@Entity
@Immutable
@Table(name = "wallet_movements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", updatable = false)
    private WalletAccount wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private MovementType type;

    @Column(nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, updatable = false)
    private String description;

    @Column(updatable = false, length = 100)
    private String reference;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    WalletMovement(WalletAccount wallet, MovementType type, BigDecimal amount, BigDecimal balanceAfter,
            String description, String reference) {
        this.wallet = wallet;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.reference = reference;
        this.createdAt = LocalDateTime.now();
    }

    /** Monto con signo: positivo si entra dinero, negativo si sale. */
    public BigDecimal signedAmount() {
        return type.isCredit() ? amount : amount.negate();
    }
}
