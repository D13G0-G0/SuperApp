package co.edu.unal.superapp.dto;

import co.edu.unal.superapp.entity.Listing;
import java.math.BigDecimal;
import java.time.Instant;

/** DTO de salida: lo que el cliente recibe (sin exponer la entity). */
public record ListingResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        boolean available,
        Instant createdAt) {

    public static ListingResponse from(Listing l) {
        return new ListingResponse(l.getId(), l.getTitle(), l.getDescription(),
                l.getPrice(), l.isAvailable(), l.getCreatedAt());
    }
}
