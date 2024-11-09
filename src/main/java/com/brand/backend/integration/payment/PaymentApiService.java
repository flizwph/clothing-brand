package com.brand.backend.integration.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentApiService {

    private final PaymentApiClient paymentApiClient;

    @Value("${payment.api.url}")
    private String apiUrl;

    @Value("${payment.api.secret}")
    private String secretKey;

    public String createPayment(PaymentRequest request) {
        String endpoint = apiUrl + "/payments";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(request.getMerchantId(), secretKey);
        headers.set("Idempotency-Key", request.getIdempotencyKey());
        headers.set("Content-Type", "application/json");

        ResponseEntity<String> response = paymentApiClient.sendPostRequest(endpoint, headers, request.toJson());
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody(); // parse response to get payment details
        } else {
            throw new RuntimeException("Payment creation failed: " + response.getBody());
        }
    }

    public String getPaymentStatus(String paymentId) {
        String endpoint = apiUrl + "/payments/" + paymentId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("your-merchant-id", secretKey);

        ResponseEntity<String> response = paymentApiClient.sendGetRequest(endpoint, headers);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody(); // parse response to get payment status
        } else {
            throw new RuntimeException("Failed to get payment status: " + response.getBody());
        }
    }
}
