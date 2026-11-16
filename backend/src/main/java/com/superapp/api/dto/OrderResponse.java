package com.superapp.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.superapp.api.entity.OrderStatus;

public record OrderResponse(
        Long id,
        OrderStatus status,
        Long listingId,
        String title,
        String description,
        String imageUrl,
        String pickupAddress,
        BigDecimal total,
        BigDecimal deliveryFee,
        String deliveryAddress,
        Long customerId,
        String customerName,
        Long driverId,
        String driverName,
        LocalDateTime createdAt
) {
}
