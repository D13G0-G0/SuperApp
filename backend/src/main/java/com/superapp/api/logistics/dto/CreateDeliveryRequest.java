package com.superapp.api.logistics.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * Dos formas de pedir:
 *  1) Del catálogo: se envía listingId (lo demás se ignora).
 *  2) Pedido libre: se envía title, pickupAddress y price (description es opcional).
 */
public record CreateDeliveryRequest(
        Long listingId,

        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String title,

        @Size(max = 1000, message = "El detalle no puede superar 1000 caracteres")
        String description,

        @Size(max = 300, message = "La dirección de recogida no puede superar 300 caracteres")
        String pickupAddress,

        @DecimalMin(value = "0.01", message = "El valor debe ser mayor que 0")
        BigDecimal price
) {
}
