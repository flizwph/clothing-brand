package com.brand.backend.controllers;

import com.brand.backend.dtos.PaymentDto;
import com.brand.backend.integration.payment.PaymentApiService;
import com.brand.backend.integration.payment.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentApiService paymentApiService;

    @PostMapping("/create")
    public ResponseEntity<String> createPayment(@RequestBody PaymentDto paymentDto) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setMerchantId(paymentDto.getMerchantId());
        paymentRequest.setIdempotencyKey(paymentDto.getIdempotencyKey());
        paymentRequest.setAmount(paymentDto.getAmount());
        paymentRequest.setCurrency(paymentDto.getCurrency());
        paymentRequest.setDescription(paymentDto.getDescription());
        paymentRequest.setConfirmationType(paymentDto.getConfirmationType());
        paymentRequest.setReturnUrl(paymentDto.getReturnUrl());

        String response = paymentApiService.createPayment(paymentRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<String> getPaymentStatus(@PathVariable String paymentId) {
        String status = paymentApiService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(status);
    }
}
