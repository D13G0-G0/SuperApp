package com.superapp.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.superapp.api.catalog.domain.Listing;
import com.superapp.api.catalog.domain.ListingStatus;
import com.superapp.api.catalog.dto.ListingRequest;
import com.superapp.api.catalog.dto.ListingResponse;
import com.superapp.api.catalog.repository.ListingRepository;
import com.superapp.api.catalog.service.ListingReferences;
import com.superapp.api.catalog.service.ListingService;
import com.superapp.api.identity.domain.Role;
import com.superapp.api.identity.domain.User;
import com.superapp.api.identity.repository.UserRepository;
import com.superapp.api.shared.exception.ForbiddenOperationException;
import com.superapp.api.shared.exception.ResourceNotFoundException;

/** Pruebas unitarias de las reglas del catálogo, sin base de datos (repositorios simulados con Mockito). */
class ListingServiceTest {

    private static final ListingRequest REQUEST = new ListingRequest(
            " Silla ", "Silla de madera", new BigDecimal("50000"), "Hogar", "https://picsum.photos/seed/s/600/400");

    private ListingRepository listings;
    private UserRepository users;
    private ListingReferences references;
    private ListingService service;
    private User seller;
    private User otherSeller;

    @BeforeEach
    void setUp() {
        listings = mock(ListingRepository.class);
        users = mock(UserRepository.class);
        references = mock(ListingReferences.class);
        service = new ListingService(listings, users, List.of(references));
        seller = user(1L, Role.CUSTOMER, Role.SELLER);
        otherSeller = user(2L, Role.SELLER);
        when(listings.save(any(Listing.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static User user(Long id, Role... roles) {
        User u = new User();
        u.setId(id);
        u.setFullName("Usuario " + id);
        u.setRoles(Set.of(roles));
        return u;
    }

    private Listing listing(Long id, User owner, ListingStatus status) {
        Listing l = new Listing();
        l.setId(id);
        l.setTitle("Viejo");
        l.setDescription("desc");
        l.setPrice(BigDecimal.TEN);
        l.setCategory("Otros");
        l.setImageUrl("https://x.test/a.png");
        l.setStatus(status);
        l.setSeller(owner);
        when(listings.findById(id)).thenReturn(Optional.of(l));
        return l;
    }

    @Test
    void sellerCreatesPublishedListing() {
        when(users.findById(1L)).thenReturn(Optional.of(seller));
        ListingResponse r = service.createListing(1L, REQUEST);
        assertThat(r.title()).isEqualTo("Silla");
        assertThat(r.status()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(r.sellerId()).isEqualTo(1L);
    }

    @Test
    void customerWithoutSellerRoleCannotPublish() {
        when(users.findById(3L)).thenReturn(Optional.of(user(3L, Role.CUSTOMER)));
        assertThatThrownBy(() -> service.createListing(3L, REQUEST))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(listings, never()).save(any());
    }

    @Test
    void ownerUpdatesListing() {
        listing(10L, seller, ListingStatus.PUBLISHED);
        ListingResponse r = service.updateListing(1L, 10L, REQUEST);
        assertThat(r.title()).isEqualTo("Silla");
        assertThat(r.price()).isEqualByComparingTo("50000");
    }

    @Test
    void onlyOwnerCanUpdateOrDelete() {
        listing(10L, otherSeller, ListingStatus.PUBLISHED);
        assertThatThrownBy(() -> service.updateListing(1L, 10L, REQUEST))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.deleteListing(1L, 10L))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(listings, never()).delete(any());
    }

    @Test
    void soldListingCannotBeEdited() {
        listing(10L, seller, ListingStatus.SOLD);
        assertThatThrownBy(() -> service.updateListing(1L, 10L, REQUEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteWithoutOrdersRemovesRow() {
        Listing l = listing(10L, seller, ListingStatus.PUBLISHED);
        when(references.isReferenced(10L)).thenReturn(false);
        assertThat(service.deleteListing(1L, 10L)).isFalse();
        verify(listings).delete(l);
    }

    @Test
    void deleteWithOrdersIsLogical() {
        Listing l = listing(10L, seller, ListingStatus.PUBLISHED);
        when(references.isReferenced(10L)).thenReturn(true);
        assertThat(service.deleteListing(1L, 10L)).isTrue();
        assertThat(l.getStatus()).isEqualTo(ListingStatus.DELETED);
        verify(listings, never()).delete(any());
    }

    @Test
    void deletedListingIsNotFound() {
        listing(10L, seller, ListingStatus.DELETED);
        assertThatThrownBy(() -> service.getListing(10L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateListing(1L, 10L, REQUEST)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchRejectsInvertedPriceRange() {
        assertThatThrownBy(() -> service.search("", "", BigDecimal.TEN, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
