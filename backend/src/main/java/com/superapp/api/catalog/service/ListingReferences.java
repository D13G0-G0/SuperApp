package com.superapp.api.catalog.service;

/**
 * "¿Alguien usa esta publicación?" Cada módulo que guarda pedidos (logistics, orders...)
 * implementa esta interfaz. Así catálogo no depende de esos módulos y decide solo
 * si borra la publicación de verdad o la marca como DELETED.
 */
public interface ListingReferences {

    boolean isReferenced(Long listingId);
}
