package com.superapp.api.catalog.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.catalog.dto.CreateListingRequest;
import com.superapp.api.catalog.dto.ListingResponse;
import com.superapp.api.catalog.domain.Listing;
import com.superapp.api.catalog.domain.ListingStatus;
import com.superapp.api.identity.domain.Role;
import com.superapp.api.identity.domain.User;
import com.superapp.api.shared.exception.ForbiddenOperationException;
import com.superapp.api.shared.exception.ResourceNotFoundException;
import com.superapp.api.catalog.repository.ListingRepository;
import com.superapp.api.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional
    public ListingResponse createListing(Long sellerId, CreateListingRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + sellerId));

        // Pueden publicar los vendedores y también los clientes
        if (!seller.getRoles().contains(Role.SELLER) && !seller.getRoles().contains(Role.CUSTOMER)) {
            throw new ForbiddenOperationException("Solo un cliente o un vendedor puede publicar productos");
        }

        Listing listing = new Listing();
        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setPrice(request.price());
        listing.setCategory(request.category());
        listing.setImageUrl(request.imageUrl());
        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setSeller(seller);

        return toResponse(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public ListingResponse getListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada: " + listingId));
        return toResponse(listing);
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