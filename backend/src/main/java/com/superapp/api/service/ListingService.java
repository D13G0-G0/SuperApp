package com.superapp.api.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.dto.CreateListingRequest;
import com.superapp.api.dto.ListingResponse;
import com.superapp.api.entity.Listing;
import com.superapp.api.entity.ListingStatus;
import com.superapp.api.entity.Role;
import com.superapp.api.entity.User;
import com.superapp.api.exception.ForbiddenOperationException;
import com.superapp.api.exception.ResourceNotFoundException;
import com.superapp.api.repository.ListingRepository;
import com.superapp.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional
    public ListingResponse createListing(CreateListingRequest request) {
        User seller = userRepository.findById(request.sellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + request.sellerId()));

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