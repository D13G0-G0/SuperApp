package com.superapp.api.orders.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.superapp.api.orders.domain.Order;
import com.superapp.api.orders.domain.OrderStateMachine;
import com.superapp.api.orders.domain.OrderStatus;

/**
 * Un pedido como lo ve quien pregunta. nextStatuses = a qué estados puede pasarlo ESA persona
 * (la pantalla usa esto para mostrar solo los botones válidos).
 */
public record OrderResponse(
        Long id,
        Long buyerId,
        Long sellerId,
        BigDecimal total,
        OrderStatus status,
        List<Item> items,
        List<OrderStatus> nextStatuses,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record Item(Long listingId, String title, BigDecimal price) {
    }

    public static OrderResponse of(Order o, Long viewerId) {
        OrderStateMachine.Party party = o.partyOf(viewerId);
        return new OrderResponse(o.getId(), o.getBuyerId(), o.getSellerId(), o.getTotal(), o.getStatus(),
                o.getItems().stream().map(i -> new Item(i.getListingId(), i.getTitle(), i.getPrice())).toList(),
                party == null ? List.of() : OrderStateMachine.allowed(o.getStatus(), party),
                o.getCreatedAt(), o.getUpdatedAt());
    }
}
