package com.superapp.api.logistics.controller;

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

import com.superapp.api.logistics.dto.CreateDeliveryRequest;
import com.superapp.api.logistics.dto.DeliveryResponse;
import com.superapp.api.logistics.dto.DeliveryStatusRequest;
import com.superapp.api.logistics.service.DeliveryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService orderService;

    // Cliente: hace un pedido
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryResponse create(@Valid @RequestBody CreateDeliveryRequest request) {
        return orderService.create(request);
    }

    // Conductor: pedidos que aún no tienen conductor
    @GetMapping("/available")
    public List<DeliveryResponse> available() {
        return orderService.available();
    }

    // Cliente: sus pedidos
    @GetMapping("/customer/{customerId}")
    public List<DeliveryResponse> byCustomer(@PathVariable Long customerId) {
        return orderService.byCustomer(customerId);
    }

    // Conductor: sus entregas
    @GetMapping("/driver/{driverId}")
    public List<DeliveryResponse> byDriver(@PathVariable Long driverId) {
        return orderService.byDriver(driverId);
    }

    // Cambia el estado: ACCEPTED, PICKED_UP, DELIVERED (conductor) o CANCELLED (cliente)
    @PatchMapping("/{orderId}/status")
    public DeliveryResponse changeStatus(@PathVariable Long orderId,
                                      @Valid @RequestBody DeliveryStatusRequest request) {
        return orderService.changeStatus(orderId, request);
    }
}
