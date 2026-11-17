package com.superapp.api.catalog.domain;

public enum ListingStatus {
    PUBLISHED,   // visible y disponible para comprar
    SOLD,        // ya vendida
    SUSPENDED,   // retirada por un administrador
    DELETED      // borrada por el vendedor pero con pedidos: se guarda para el historial
}