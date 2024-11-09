package com.brand.backend.integration.payment;

import com.brand.backend.models.Order;
import com.brand.backend.models.Payment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class BankPaymentProvider implements PaymentProvider {

    private static final String PAYMENT_API_URL = "https://tome.ge/api/v1/payments";
    private static final String SHOP_ID = "<Ваш идентификатор магазина>";
    private static final String SECRET_KEY = "<Ваш секретный ключ>";

    @Override
    public void processPayment(Order order, Payment payment) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("amount", Map.of("value", payment.getAmount(), "currency", "RUB"));
        paymentRequest.put("confirmation", Map.of("type", "redirect", "return_url", "https://merchant.site/result"));
        paymentRequest.put("description", "Order #" + order.getOrderNumber());
        paymentRequest.put("customer", Map.of("settlement_method", "card"));

        try {
            restTemplate.postForEntity(PAYMENT_API_URL, paymentRequest, String.class);
            // Логика для обработки ответа
        } catch (Exception e) {
            throw new RuntimeException("Payment API request failed", e);
        }
    }
}
