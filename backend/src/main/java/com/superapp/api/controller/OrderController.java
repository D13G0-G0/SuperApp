package com.superapp.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.superapp.api.dto.CreateOrderRequest;
import com.superapp.api.dto.OrderResponse;
import com.superapp.api.dto.OrderStatusRequest;
import com.superapp.api.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Cliente: hace un pedido
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    // Conductor: pedidos que aún no tienen conductor
    @GetMapping("/available")
    public List<OrderResponse> available() {
        return orderService.available();
    }

    // Cliente: sus pedidos
    @GetMapping("/customer/{customerId}")
    public List<OrderResponse> byCustomer(@PathVariable Long customerId) {
        return orderService.byCustomer(customerId);
    }

    // Conductor: sus entregas
    @GetMapping("/driver/{driverId}")
    public List<OrderResponse> byDriver(@PathVariable Long driverId) {
        return orderService.byDriver(driverId);
    }

    // Cambia el estado: ACCEPTED, PICKED_UP, DELIVERED (conductor) o CANCELLED (cliente)
    @PatchMapping("/{orderId}/status")
    public OrderResponse changeStatus(@PathVariable Long orderId,
                                      @Valid @RequestBody OrderStatusRequest request) {
        return orderService.changeStatus(orderId, request);
    }
}
