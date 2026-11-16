package com.superapp.api.dto;

import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(
        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String addressLine,

        @Size(max = 100, message = "La ciudad no puede superar 100 caracteres")
        String city,

        @Size(max = 100, message = "El departamento no puede superar 100 caracteres")
        String state,

        @Size(max = 20, message = "El código postal no puede superar 20 caracteres")
        String postalCode,

        @Size(max = 100, message = "El país no puede superar 100 caracteres")
        String country
) {
}