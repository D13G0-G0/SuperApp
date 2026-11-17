package com.superapp.api.orders.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.superapp.api.orders.domain.OrderStateMachine.Party;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Pedido de compra: los productos de UN vendedor pagados por UN comprador (FR-05).
 * El estado solo cambia con {@link #moveTo}, que pasa por la {@link OrderStateMachine}.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false, updatable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private Long sellerId;

    @Column(nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    /** Pedido recién pagado. */
    public Order(Long buyerId, Long sellerId) {
        if (buyerId.equals(sellerId)) {
            throw new IllegalArgumentException("No puedes comprar tus propias publicaciones");
        }
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.status = OrderStatus.PAID;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void addItem(Long listingId, String title, BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que 0");
        }
        items.add(new OrderItem(this, listingId, title, price));
        total = total.add(price);
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** ¿Qué papel tiene este usuario en el pedido? null si no es ni comprador ni vendedor. */
    public Party partyOf(Long userId) {
        if (buyerId.equals(userId)) {
            return Party.BUYER;
        }
        if (sellerId.equals(userId)) {
            return Party.SELLER;
        }
        return null;
    }

    /** Cambia el estado si la máquina de estados lo permite para esa parte. */
    public void moveTo(OrderStatus next, Party party) {
        OrderStateMachine.check(status, next, party);
        this.status = next;
        this.updatedAt = LocalDateTime.now();
    }
}
