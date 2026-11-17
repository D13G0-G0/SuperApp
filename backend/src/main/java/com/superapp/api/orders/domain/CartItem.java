package com.superapp.api.orders.domain;

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

/** Una publicación en el carrito de un usuario. Guarda solo ids: orders no depende de identity ni catalog. */
@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "listing_id", nullable = false, updatable = false)
    private Long listingId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public CartItem(Long userId, Long listingId) {
        this.userId = userId;
        this.listingId = listingId;
        this.addedAt = LocalDateTime.now();
    }
}
