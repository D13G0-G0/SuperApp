package com.superapp.api.wallet.service;

import org.springframework.stereotype.Service;

import com.superapp.api.wallet.dto.PaymentRequest;
import com.superapp.api.wallet.dto.PaymentResponse;
import com.superapp.api.wallet.payment.PaymentResult;
import com.superapp.api.wallet.payment.PaymentStrategyFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentStrategyFactory strategyFactory;

    public PaymentResponse simulatePayment(PaymentRequest request) {
        PaymentResult result = strategyFactory
                .get(request.method())
                .pay(request.amount());

        return new PaymentResponse(
                result.method(),
                result.approved(),
                result.simulated(),
                result.transactionId(),
                result.fee(),
                result.message());
    }
}