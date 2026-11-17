package com.superapp.api.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.superapp.api.orders.domain.InvalidTransitionException;
import com.superapp.api.orders.domain.Order;
import com.superapp.api.orders.domain.OrderStateMachine;
import com.superapp.api.orders.domain.OrderStateMachine.Party;
import com.superapp.api.orders.domain.OrderStatus;

/** Estados del pedido (FR-06) sin base de datos ni servidor. */
class OrderStateMachineTest {

    private static Order paidOrder() {
        Order o = new Order(1L, 2L);
        o.addItem(10L, "Silla", new BigDecimal("50000"));
        return o;
    }

    @Test
    void happyPath() {
        Order o = paidOrder();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PAID);
        o.moveTo(OrderStatus.SHIPPED, Party.SELLER);
        o.moveTo(OrderStatus.DELIVERED, Party.BUYER);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void whoCanDoWhat() {
        assertThat(OrderStateMachine.allowed(OrderStatus.PAID, Party.SELLER))
                .containsExactly(OrderStatus.SHIPPED, OrderStatus.CANCELLED);
        assertThat(OrderStateMachine.allowed(OrderStatus.PAID, Party.BUYER)).containsExactly(OrderStatus.CANCELLED);
        assertThat(OrderStateMachine.allowed(OrderStatus.SHIPPED, Party.BUYER)).containsExactly(OrderStatus.DELIVERED);
        assertThat(OrderStateMachine.allowed(OrderStatus.SHIPPED, Party.SELLER)).isEmpty();
        assertThat(OrderStateMachine.allowed(OrderStatus.DELIVERED, Party.BUYER)).isEmpty();
        assertThat(OrderStateMachine.allowed(OrderStatus.CANCELLED, Party.SELLER)).isEmpty();
    }

    @Test
    void buyerCannotMarkAsShipped() {
        Order o = paidOrder();
        assertThatThrownBy(() -> o.moveTo(OrderStatus.SHIPPED, Party.BUYER))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("puedes pasar a: CANCELLED");
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void cannotSkipShipping() {
        Order o = paidOrder();
        assertThatThrownBy(() -> o.moveTo(OrderStatus.DELIVERED, Party.BUYER))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void cannotCancelAfterShipping() {
        Order o = paidOrder();
        o.moveTo(OrderStatus.SHIPPED, Party.SELLER);
        assertThatThrownBy(() -> o.moveTo(OrderStatus.CANCELLED, Party.BUYER))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("DELIVERED");
    }

    @Test
    void finalStatesAreFinal() {
        Order cancelled = paidOrder();
        cancelled.moveTo(OrderStatus.CANCELLED, Party.BUYER);
        assertThatThrownBy(() -> cancelled.moveTo(OrderStatus.PAID, Party.SELLER))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("no puedes hacer más cambios");
        assertThatThrownBy(() -> cancelled.moveTo(OrderStatus.CANCELLED, Party.SELLER))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void sellerCanCancelBeforeShipping() {
        Order o = paidOrder();
        assertThatCode(() -> o.moveTo(OrderStatus.CANCELLED, Party.SELLER)).doesNotThrowAnyException();
    }

    @Test
    void orderRules() {
        assertThatThrownBy(() -> new Order(5L, 5L)).hasMessageContaining("propias");
        Order o = new Order(1L, 2L);
        o.addItem(1L, "A", new BigDecimal("1000"));
        o.addItem(2L, "B", new BigDecimal("2500.50"));
        assertThat(o.getTotal()).isEqualByComparingTo("3500.50");
        assertThatThrownBy(() -> o.addItem(3L, "C", BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThat(o.partyOf(1L)).isEqualTo(Party.BUYER);
        assertThat(o.partyOf(2L)).isEqualTo(Party.SELLER);
        assertThat(o.partyOf(3L)).isNull();
    }
}
