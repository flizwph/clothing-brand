package com.brand.backend.integration.payment;

import com.brand.backend.models.Order;
import com.brand.backend.models.Payment;

public interface PaymentProvider {
    void processPayment(Order order, Payment payment);
}
