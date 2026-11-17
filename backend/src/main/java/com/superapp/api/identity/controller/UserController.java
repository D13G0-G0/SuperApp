package com.superapp.api.identity.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.superapp.api.identity.dto.ShippingAddressRequest;
import com.superapp.api.identity.dto.UserResponse;
import com.superapp.api.identity.service.UserService;
import com.superapp.api.shared.security.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    /** Mi perfil. */
    @GetMapping("/me")
    public UserResponse me() {
        return userService.getUser(currentUser.id());
    }

    /** Cambiar mi dirección de entrega. */
    @PatchMapping("/me/shipping-address")
    public UserResponse updateMyShippingAddress(@Valid @RequestBody ShippingAddressRequest request) {
        return userService.updateShippingAddress(currentUser.id(), request);
    }

    /** Ver cualquier usuario: solo ADMIN (regla en SecurityConfig). */
    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {
        return userService.getUser(userId);
    }
}
