package com.superapp.api.orders.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.superapp.api.orders.dto.AddToCartRequest;
import com.superapp.api.orders.dto.CartResponse;
import com.superapp.api.orders.service.CartService;
import com.superapp.api.shared.security.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Carrito del comprador (rol CUSTOMER, ver SecurityConfig: /api/cart/**). */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    @GetMapping
    public CartResponse cart() {
        return cartService.view(currentUser.id());
    }

    @PostMapping("/items")
    public CartResponse add(@Valid @RequestBody AddToCartRequest request) {
        return cartService.add(currentUser.id(), request.listingId());
    }

    @DeleteMapping("/items/{listingId}")
    public CartResponse remove(@PathVariable Long listingId) {
        return cartService.remove(currentUser.id(), listingId);
    }
}
