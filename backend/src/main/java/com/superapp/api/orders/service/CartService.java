package com.superapp.api.orders.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.catalog.api.ICatalog;
import com.superapp.api.catalog.api.ICatalog.ListingView;
import com.superapp.api.orders.domain.CartItem;
import com.superapp.api.orders.dto.CartResponse;
import com.superapp.api.orders.dto.CartResponse.CartLine;
import com.superapp.api.orders.repository.CartItemRepository;
import com.superapp.api.shared.exception.ResourceNotFoundException;
import com.superapp.api.wallet.api.IWallet;

import lombok.RequiredArgsConstructor;

/** Carrito (FR-05): agregar, quitar y ver. Habla con catálogo y billetera solo por sus interfaces. */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cart;
    private final ICatalog catalog;
    private final IWallet wallet;

    @Transactional(readOnly = true)
    public CartResponse view(Long userId) {
        List<CartItem> items = cart.findByUserIdOrderByAddedAtAscIdAsc(userId);
        Map<Long, ListingView> listings = catalog.find(items.stream().map(CartItem::getListingId).toList())
                .stream().collect(Collectors.toMap(ListingView::id, Function.identity()));
        List<CartLine> lines = items.stream()
                .map(i -> listings.get(i.getListingId()))
                .filter(l -> l != null)
                .map(l -> new CartLine(l.id(), l.title(), l.price(), l.imageUrl(), l.sellerId(), l.sellerName(),
                        l.available() && !l.sellerId().equals(userId)))
                .toList();
        BigDecimal total = lines.stream().filter(CartLine::available).map(CartLine::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = wallet.balance(userId);
        return new CartResponse(lines, total, balance, balance.compareTo(total) >= 0);
    }

    @Transactional
    public CartResponse add(Long userId, Long listingId) {
        ListingView listing = catalog.find(List.of(listingId)).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada"));
        if (listing.sellerId().equals(userId)) {
            throw new IllegalArgumentException("No puedes comprar tus propias publicaciones");
        }
        if (!listing.available()) {
            throw new IllegalArgumentException("Esta publicación ya no está disponible");
        }
        if (!cart.existsByUserIdAndListingId(userId, listingId)) {
            cart.save(new CartItem(userId, listingId));
        }
        return view(userId);
    }

    @Transactional
    public CartResponse remove(Long userId, Long listingId) {
        cart.deleteItems(userId, List.of(listingId));
        return view(userId);
    }
}
