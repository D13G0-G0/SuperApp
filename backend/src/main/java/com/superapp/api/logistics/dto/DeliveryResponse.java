package com.superapp.api.logistics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.superapp.api.logistics.domain.DeliveryStatus;

public record DeliveryResponse(
        Long id,
        DeliveryStatus status,
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
