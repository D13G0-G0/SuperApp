package com.superapp.api.orders.domain;

import java.util.List;

/**
 * Alguna publicación del carrito ya se vendió, se borró o es del mismo comprador.
 * La compra no se hace y esas publicaciones salen del carrito. Responde 400.
 */
public class ListingUnavailableException extends IllegalArgumentException {

    private final List<String> titles;

    public ListingUnavailableException(List<String> titles) {
        super("La publicación ya no está disponible: " + String.join(", ", titles)
                + ". La quitamos de tu carrito y no se cobró nada.");
        this.titles = titles;
    }

    public List<String> getTitles() {
        return titles;
    }
}
