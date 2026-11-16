package com.superapp.api.service;

import org.springframework.stereotype.Service;

import com.superapp.api.dto.PaymentRequest;
import com.superapp.api.dto.PaymentResponse;
import com.superapp.api.payment.PaymentResult;
import com.superapp.api.payment.PaymentStrategyFactory;

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