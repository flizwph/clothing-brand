package com.brand.backend.domain.order.model;

public enum OrderStatus {
    NEW,         // Новый заказ
    PROCESSING,  // Заказ в обработке
    DISPATCHED,
    COMPLETED,   // Заказ выполнен
    CANCELLED    // Заказ отменен
}
