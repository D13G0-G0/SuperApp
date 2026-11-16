package com.superapp.api.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.superapp.api.catalog.domain.ListingStatus;

public record ListingResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        String category,
        String imageUrl,
        ListingStatus status,
        Long sellerId,
        String sellerName,
        LocalDateTime createdAt
) {
}