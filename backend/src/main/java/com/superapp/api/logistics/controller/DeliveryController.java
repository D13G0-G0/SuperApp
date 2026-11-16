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

import com.superapp.api.shared.security.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final CurrentUser currentUser;

    // Cliente: hace un pedido
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryResponse create(@Valid @RequestBody CreateDeliveryRequest request) {
        return deliveryService.create(currentUser.id(), request);
    }

    // Conductor: pedidos que aún no tienen conductor
    @GetMapping("/available")
    public List<DeliveryResponse> available() {
        return deliveryService.available();
    }

    // Cliente: sus pedidos
    @GetMapping("/mine")
    public List<DeliveryResponse> mine() {
        return deliveryService.byCustomer(currentUser.id());
    }

    // Conductor: sus entregas
    @GetMapping("/assigned")
    public List<DeliveryResponse> assigned() {
        return deliveryService.byDriver(currentUser.id());
    }

    // Cambia el estado: ACCEPTED, PICKED_UP, DELIVERED (conductor) o CANCELLED (cliente)
    @PatchMapping("/{orderId}/status")
    public DeliveryResponse changeStatus(@PathVariable Long orderId,
                                      @Valid @RequestBody DeliveryStatusRequest request) {
        return deliveryService.changeStatus(currentUser.id(), orderId, request);
    }
}
