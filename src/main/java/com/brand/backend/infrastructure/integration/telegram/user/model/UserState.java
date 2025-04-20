package com.brand.backend.infrastructure.integration.telegram.user.model;

public enum UserState {
    NONE,
    WAITING_SEARCH_INPUT,
    WAITING_CHECKOUT_NAME,
    WAITING_CHECKOUT_PHONE,
    WAITING_CHECKOUT_ADDRESS,
    WAITING_CHECKOUT_CONFIRMATION
} 