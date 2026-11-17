package com.superapp.api.logistics.service;

import org.springframework.stereotype.Component;

import com.superapp.api.catalog.service.ListingReferences;
import com.superapp.api.logistics.repository.DeliveryOrderRepository;

import lombok.RequiredArgsConstructor;

/** Una publicación con entregas no se puede borrar de verdad (se perdería el historial). */
@Component
@RequiredArgsConstructor
public class DeliveryListingReferences implements ListingReferences {

    private final DeliveryOrderRepository deliveryOrderRepository;

    @Override
    public boolean isReferenced(Long listingId) {
        return deliveryOrderRepository.existsByListingId(listingId);
    }
}
