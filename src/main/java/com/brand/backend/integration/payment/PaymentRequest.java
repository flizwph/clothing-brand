package com.brand.backend.integration.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {
    private String merchantId;
    private String idempotencyKey;
    private double amount;
    private String currency;
    private String description;
    private String confirmationType;
    private String returnUrl;

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert request to JSON", e);
        }
    }
}
