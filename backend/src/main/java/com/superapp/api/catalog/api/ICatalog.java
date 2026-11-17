package com.superapp.api.catalog.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * Interfaz pública del catálogo para otros módulos (orders). Así orders nunca toca
 * el repositorio ni la entidad Listing. La implementación vive en catalog.
 */
public interface ICatalog {

    /** Datos de publicaciones para mostrarlas (sin bloquear). Las que no existen no aparecen. */
    List<ListingView> find(Collection<Long> listingIds);

    /**
     * Lee las publicaciones bloqueando sus filas (SELECT ... FOR UPDATE) hasta el final de la
     * transacción de quien llama. Se bloquean en orden de id para que dos compras no se esperen
     * en círculo (deadlock). Mientras dure el bloqueo nadie más puede comprarlas ni editarlas.
     */
    List<ListingView> lockForPurchase(Collection<Long> listingIds);

    /** Marca como vendidas publicaciones ya bloqueadas con {@link #lockForPurchase}. */
    void markSold(Collection<Long> listingIds);

    /** Devuelve a la venta (PUBLISHED) publicaciones vendidas, p. ej. al cancelar un pedido. */
    void release(Collection<Long> listingIds);

    /** Vista de una publicación para otros módulos. available = se puede comprar ahora. */
    record ListingView(Long id, String title, BigDecimal price, String imageUrl, Long sellerId,
            String sellerName, boolean available) {
    }
}
