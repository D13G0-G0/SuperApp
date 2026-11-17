package com.superapp.api.orders.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.superapp.api.orders.dto.CheckoutResponse;
import com.superapp.api.orders.dto.OrderResponse;
import com.superapp.api.orders.dto.StatusChangeRequest;
import com.superapp.api.orders.service.CheckoutService;
import com.superapp.api.orders.service.OrderService;
import com.superapp.api.shared.security.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Compras del cliente (rol CUSTOMER, ver SecurityConfig: /api/orders/**). */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final CurrentUser currentUser;

    /** Paga todo el carrito con la billetera, en una sola transacción. */
    @PostMapping("/checkout")
    public CheckoutResponse checkout() {
        return checkoutService.checkout(currentUser.id());
    }

    /** "Mis compras". */
    @GetMapping
    public List<OrderResponse> purchases() {
        return orderService.purchases(currentUser.id());
    }

    /** El comprador confirma la entrega (DELIVERED) o cancela (CANCELLED). */
    @PatchMapping("/{orderId}/status")
    public OrderResponse changeStatus(@PathVariable Long orderId, @Valid @RequestBody StatusChangeRequest request) {
        return orderService.changeStatus(currentUser.id(), orderId, request.status());
    }
}
