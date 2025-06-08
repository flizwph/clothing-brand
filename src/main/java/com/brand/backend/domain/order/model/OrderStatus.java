package com.brand.backend.domain.order.model;

public enum OrderStatus {
    NEW,         // New order
    PROCESSING,  // Order in processing
    DISPATCHED,  // Order dispatched
    COMPLETED,   // Order completed
    CANCELLED    // Order cancelled
}
