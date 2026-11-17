package com.superapp.api.orders.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de una compra: un pedido por vendedor, lo cobrado y el saldo que quedó. */
public record CheckoutResponse(List<OrderResponse> orders, BigDecimal charged, BigDecimal balance) {
}
