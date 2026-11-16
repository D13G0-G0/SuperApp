package com.superapp.api.wallet.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.superapp.api.wallet.dto.PaymentRequest;
import com.superapp.api.wallet.dto.PaymentResponse;
import com.superapp.api.wallet.payment.PaymentMethod;
import com.superapp.api.wallet.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/methods")
    public List<PaymentMethod> methods() {
        return Arrays.asList(PaymentMethod.values());
    }

    @PostMapping("/simulate")
    public PaymentResponse simulate(@Valid @RequestBody PaymentRequest request) {
        return paymentService.simulatePayment(request);
    }
}