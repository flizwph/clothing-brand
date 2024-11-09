package com.brand.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentDto {
    private Long orderId;           // ID заказа, который нужно оплатить
    private String paymentMethod;   // Способ оплаты (например, "BANK_CARD")
    private double amount;          // Сумма платежа
    private String currency;        // Валюта (например, "RUB")
    private String description;     // Описание транзакции (например, "Оплата заказа №12")
    private String confirmationType; // Тип подтверждения (например, "redirect")
    private String returnUrl;       // URL, на который вернется клиент после оплаты
    private String merchantId;      // Идентификатор магазина
    private String idempotencyKey;  // Ключ идемпотентности

    // Конструктор
    public PaymentDto(Long orderId, String paymentMethod, double amount, String description, String returnUrl, String currency, String confirmationType, String merchantId, String idempotencyKey) {
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.description = description;
        this.returnUrl = returnUrl;
        this.currency = currency;
        this.confirmationType = confirmationType;
        this.merchantId = merchantId;
        this.idempotencyKey = idempotencyKey;
    }
}

