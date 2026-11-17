package com.superapp.api.orders.dto;

import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(@NotNull(message = "Falta la publicación") Long listingId) {
}
