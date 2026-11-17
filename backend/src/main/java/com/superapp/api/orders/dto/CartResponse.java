package com.superapp.api.orders.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carrito del usuario. total suma solo lo disponible; balance es el saldo de la billetera,
 * para que la pantalla diga de una vez si alcanza.
 */
public record CartResponse(List<CartLine> items, BigDecimal total, BigDecimal balance, boolean enoughBalance) {

    public record CartLine(Long listingId, String title, BigDecimal price, String imageUrl, Long sellerId,
            String sellerName, boolean available) {
    }
}
