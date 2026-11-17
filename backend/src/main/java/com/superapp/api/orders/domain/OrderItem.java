package com.superapp.api.orders.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Un producto dentro de un pedido, con el título y el precio que se pagaron. */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", updatable = false)
    private Order order;

    @Column(name = "listing_id", nullable = false, updatable = false)
    private Long listingId;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal price;

    OrderItem(Order order, Long listingId, String title, BigDecimal price) {
        this.order = order;
        this.listingId = listingId;
        this.title = title;
        this.price = price;
    }
}
