package com.superapp.api.orders.service;

import org.springframework.stereotype.Component;

import com.superapp.api.catalog.service.ListingReferences;
import com.superapp.api.orders.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

/** Una publicación que está en algún pedido no se borra de verdad: catálogo la marca DELETED. */
@Component
@RequiredArgsConstructor
public class OrderListingReferences implements ListingReferences {

    private final OrderRepository orders;

    @Override
    public boolean isReferenced(Long listingId) {
        return orders.isListingReferenced(listingId);
    }
}
