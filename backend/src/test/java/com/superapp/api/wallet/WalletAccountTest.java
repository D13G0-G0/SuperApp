package com.superapp.api.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.superapp.api.wallet.domain.InsufficientBalanceException;
import com.superapp.api.wallet.domain.MovementType;
import com.superapp.api.wallet.domain.WalletAccount;
import com.superapp.api.wallet.domain.WalletMovement;

/** Reglas del dinero (FR-07) sin base de datos ni servidor. */
class WalletAccountTest {

    private static BigDecimal money(String v) {
        return new BigDecimal(v).setScale(2);
    }

    @Test
    void newWalletStartsAtZero() {
        assertThat(new WalletAccount(1L).getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void topUpAddsAndRecordsBalanceAfter() {
        WalletAccount w = new WalletAccount(1L);
        WalletMovement m = w.apply(MovementType.TOP_UP, money("50000"), "Recarga", "SIM-1");

        assertThat(w.getBalance()).isEqualByComparingTo("50000");
        assertThat(m.getType()).isEqualTo(MovementType.TOP_UP);
        assertThat(m.getAmount()).isEqualByComparingTo("50000");
        assertThat(m.getBalanceAfter()).isEqualByComparingTo("50000");
        assertThat(m.signedAmount()).isEqualByComparingTo("50000");
        assertThat(m.getCreatedAt()).isNotNull();
    }

    @Test
    void paymentSubtracts() {
        WalletAccount w = new WalletAccount(1L);
        w.apply(MovementType.TOP_UP, money("50000"), "Recarga", null);
        WalletMovement m = w.apply(MovementType.PAYMENT, money("20000"), "Compra", "ORDER-1");

        assertThat(w.getBalance()).isEqualByComparingTo("30000");
        assertThat(m.signedAmount()).isEqualByComparingTo("-20000");
    }

    @Test
    void cannotSpendMoreThanTheBalance() {
        WalletAccount w = new WalletAccount(1L);
        w.apply(MovementType.TOP_UP, money("10000"), "Recarga", null);

        assertThatThrownBy(() -> w.apply(MovementType.PAYMENT, money("10000.01"), "Compra", null))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageStartingWith("Saldo insuficiente");
        assertThat(w.getBalance()).isEqualByComparingTo("10000"); // no cambió nada
    }

    @Test
    void canSpendExactlyTheBalance() {
        WalletAccount w = new WalletAccount(1L);
        w.apply(MovementType.TOP_UP, money("10000"), "Recarga", null);
        w.apply(MovementType.PAYMENT, money("10000"), "Compra", null);

        assertThat(w.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void zeroOrNegativeAmountsAreRejected() {
        WalletAccount w = new WalletAccount(1L);
        assertThatThrownBy(() -> w.apply(MovementType.TOP_UP, BigDecimal.ZERO, "x", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mayor que 0");
        assertThatThrownBy(() -> w.apply(MovementType.TOP_UP, new BigDecimal("-5"), "x", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mayor que 0");
        assertThatThrownBy(() -> w.apply(MovementType.TOP_UP, new BigDecimal("0.001"), "x", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mayor que 0");
        assertThatThrownBy(() -> w.apply(MovementType.TOP_UP, null, "x", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> w.apply(null, BigDecimal.TEN, "x", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void balanceAlwaysEqualsSignedSumOfMovements() {
        WalletAccount w = new WalletAccount(1L);
        List<WalletMovement> history = new ArrayList<>();
        history.add(w.apply(MovementType.TOP_UP, money("100000"), "Recarga", null));
        history.add(w.apply(MovementType.PAYMENT, money("35000"), "Compra", null));
        history.add(w.apply(MovementType.INCOME, money("12000"), "Venta", null));
        history.add(w.apply(MovementType.REFUND, money("35000"), "Reembolso", null));
        history.add(w.apply(MovementType.PAYMENT, money("99999.99"), "Compra", null));
        try {
            w.apply(MovementType.PAYMENT, money("1000000"), "Compra rechazada", null);
        } catch (InsufficientBalanceException expected) {
            // un intento fallido no deja movimiento ni cambia el saldo
        }

        BigDecimal sum = history.stream().map(WalletMovement::signedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(w.getBalance()).isEqualByComparingTo(sum).isEqualByComparingTo("12000.01");
        assertThat(history.get(history.size() - 1).getBalanceAfter()).isEqualByComparingTo(w.getBalance());
    }
}
