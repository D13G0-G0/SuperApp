package com.superapp.api.wallet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.superapp.api.shared.security.CurrentUser;
import com.superapp.api.wallet.dto.TopUpRequest;
import com.superapp.api.wallet.dto.TopUpResponse;
import com.superapp.api.wallet.dto.WalletResponse;
import com.superapp.api.wallet.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Billetera del usuario que hace la petición (rol CUSTOMER, ver SecurityConfig). */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final CurrentUser currentUser;

    @GetMapping
    public WalletResponse wallet() {
        return walletService.view(currentUser.id());
    }

    @PostMapping("/top-up")
    public TopUpResponse topUp(@Valid @RequestBody TopUpRequest request) {
        return walletService.topUp(currentUser.id(), request);
    }
}
