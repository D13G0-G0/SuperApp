package com.superapp.api.catalog.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.catalog.api.ICatalog;
import com.superapp.api.catalog.domain.Listing;
import com.superapp.api.catalog.domain.ListingStatus;
import com.superapp.api.catalog.repository.ListingRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/** Implementación de {@link ICatalog}: lo que el módulo orders necesita del catálogo. */
@Service
@RequiredArgsConstructor
public class CatalogFacade implements ICatalog {

    private final ListingRepository listings;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public List<ListingView> find(Collection<Long> listingIds) {
        return listings.findAllById(listingIds).stream().map(CatalogFacade::view).toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY) // el bloqueo solo tiene sentido dentro de una compra
    public List<ListingView> lockForPurchase(Collection<Long> listingIds) {
        return listingIds.stream().distinct().sorted()
                .map(id -> em.find(Listing.class, id, LockModeType.PESSIMISTIC_WRITE))
                .filter(Objects::nonNull)
                .map(CatalogFacade::view)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void markSold(Collection<Long> listingIds) {
        for (Listing l : listings.findAllById(listingIds)) {
            if (l.getStatus() != ListingStatus.PUBLISHED) {
                throw new IllegalStateException("La publicación " + l.getId() + " ya no está disponible");
            }
            l.setStatus(ListingStatus.SOLD);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Collection<Long> listingIds) {
        for (Listing l : listings.findAllById(listingIds)) {
            if (l.getStatus() == ListingStatus.SOLD) {
                l.setStatus(ListingStatus.PUBLISHED);
            }
        }
    }

    private static ListingView view(Listing l) {
        return new ListingView(l.getId(), l.getTitle(), l.getPrice(), l.getImageUrl(), l.getSeller().getId(),
                l.getSeller().getFullName(), l.getStatus() == ListingStatus.PUBLISHED);
    }
}
