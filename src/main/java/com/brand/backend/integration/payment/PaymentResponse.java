package com.brand.backend.integration.payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponse {
    private String transactionId;
    private String status;
    private double amount;
    private String currency;
    private String confirmationUrl;
    private String createdAt;
    private boolean paid;
    private boolean refundable;
}
