package com.brand.backend.integration.payment;

import org.springframework.stereotype.Component;

@Component
public class PaymentProviderFactory {

    public PaymentProvider getPaymentProvider(String paymentMethod) {
        switch (paymentMethod) {
            case "BANK_CARD":
                return new BankPaymentProvider();
            // Могут быть другие провайдеры в будущем (например, криптовалюты)
            default:
                throw new RuntimeException("Unsupported payment method");
        }
    }
}
