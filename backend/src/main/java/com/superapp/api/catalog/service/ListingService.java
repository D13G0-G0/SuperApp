package com.superapp.api.catalog.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.catalog.domain.Listing;
import com.superapp.api.catalog.domain.ListingStatus;
import com.superapp.api.catalog.dto.ListingRequest;
import com.superapp.api.catalog.dto.ListingResponse;
import com.superapp.api.catalog.repository.ListingRepository;
import com.superapp.api.identity.domain.Role;
import com.superapp.api.identity.domain.User;
import com.superapp.api.identity.repository.UserRepository;
import com.superapp.api.shared.exception.ForbiddenOperationException;
import com.superapp.api.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** Reglas del catálogo (FR-03, FR-04). No sabe nada de HTTP ni de SQL. */
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final List<ListingReferences> references;

    @Transactional
    public ListingResponse createListing(Long sellerId, ListingRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + sellerId));
        if (!seller.getRoles().contains(Role.SELLER)) {
            throw new ForbiddenOperationException("Solo un vendedor puede publicar productos");
        }
        Listing listing = new Listing();
        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setSeller(seller);
        apply(listing, request);
        return toResponse(listingRepository.save(listing));
    }

    /** Editar: solo el dueño, y no si ya se vendió. */
    @Transactional
    public ListingResponse updateListing(Long sellerId, Long listingId, ListingRequest request) {
        Listing listing = findOwned(sellerId, listingId);
        if (listing.getStatus() == ListingStatus.SOLD) {
            throw new IllegalArgumentException("Una publicación vendida no se puede editar");
        }
        apply(listing, request);
        return toResponse(listingRepository.save(listing));
    }

    /**
     * Eliminar: solo el dueño. Si algún pedido la usa, se marca DELETED (borrado lógico)
     * para no romper el historial; si no, se borra de la base.
     * @return true si fue borrado lógico
     */
    @Transactional
    public boolean deleteListing(Long sellerId, Long listingId) {
        Listing listing = findOwned(sellerId, listingId);
        boolean referenced = references.stream().anyMatch(r -> r.isReferenced(listingId));
        if (referenced) {
            listing.setStatus(ListingStatus.DELETED);
            listingRepository.save(listing);
        } else {
            listingRepository.delete(listing);
        }
        return referenced;
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> mine(Long sellerId) {
        return listingRepository.findBySellerIdAndStatusNotOrderByCreatedAtDesc(sellerId, ListingStatus.DELETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ListingResponse getListing(Long listingId) {
        return toResponse(findVisible(listingId));
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> search(String keyword, String category,
                                        BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("El precio mínimo no puede ser mayor que el máximo");
        }
        return listingRepository
                .search(ListingStatus.PUBLISHED, keyword.trim(), category.trim(), minPrice, maxPrice)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Listing findVisible(Long listingId) {
        return listingRepository.findById(listingId)
                .filter(l -> l.getStatus() != ListingStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada: " + listingId));
    }

    private Listing findOwned(Long sellerId, Long listingId) {
        Listing listing = findVisible(listingId);
        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenOperationException("Solo el dueño puede modificar esta publicación");
        }
        return listing;
    }

    private static void apply(Listing listing, ListingRequest request) {
        listing.setTitle(request.title().trim());
        listing.setDescription(request.description().trim());
        listing.setPrice(request.price());
        listing.setCategory(request.category().trim());
        listing.setImageUrl(request.imageUrl().trim());
    }

    private ListingResponse toResponse(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPrice(),
                listing.getCategory(),
                listing.getImageUrl(),
                listing.getStatus(),
                listing.getSeller().getId(),
                listing.getSeller().getFullName(),
                listing.getCreatedAt());
    }
}
