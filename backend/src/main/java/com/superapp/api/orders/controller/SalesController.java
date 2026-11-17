package com.superapp.api.orders.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.superapp.api.orders.dto.OrderResponse;
import com.superapp.api.orders.dto.StatusChangeRequest;
import com.superapp.api.orders.service.OrderService;
import com.superapp.api.shared.security.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Ventas del vendedor. Va en /api/sales (no /api/orders) porque una cuenta puede ser solo SELLER.
 * Pide sesión; el servicio solo deja ver y cambiar pedidos donde el usuario es el vendedor.
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final OrderService orderService;
    private final CurrentUser currentUser;

    /** "Mis ventas". */
    @GetMapping
    public List<OrderResponse> sales() {
        return orderService.sales(currentUser.id());
    }

    /** El vendedor marca el pedido como enviado (SHIPPED) o lo cancela (CANCELLED). */
    @PatchMapping("/{orderId}/status")
    public OrderResponse changeStatus(@PathVariable Long orderId, @Valid @RequestBody StatusChangeRequest request) {
        return orderService.changeStatus(currentUser.id(), orderId, request.status());
    }
}
