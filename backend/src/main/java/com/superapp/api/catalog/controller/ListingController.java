package com.superapp.api.catalog.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.superapp.api.catalog.dto.ListingRequest;
import com.superapp.api.catalog.dto.ListingResponse;
import com.superapp.api.catalog.service.ListingService;
import com.superapp.api.shared.security.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse createListing(@Valid @RequestBody ListingRequest request) {
        return listingService.createListing(currentUser.id(), request);
    }

    /** Mis publicaciones (SELLER). */
    @GetMapping("/mine")
    public List<ListingResponse> mine() {
        return listingService.mine(currentUser.id());
    }

    @PutMapping("/{listingId}")
    public ListingResponse updateListing(@PathVariable Long listingId,
                                         @Valid @RequestBody ListingRequest request) {
        return listingService.updateListing(currentUser.id(), listingId, request);
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteListing(@PathVariable Long listingId) {
        listingService.deleteListing(currentUser.id(), listingId);
    }

    @GetMapping
    public List<ListingResponse> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "0") BigDecimal minPrice,
            @RequestParam(defaultValue = "999999999") BigDecimal maxPrice) {
        return listingService.search(keyword, category, minPrice, maxPrice);
    }

    @GetMapping("/{listingId}")
    public ListingResponse getListing(@PathVariable Long listingId) {
        return listingService.getListing(listingId);
    }
}
