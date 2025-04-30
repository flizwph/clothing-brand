package com.brand.backend.domain.subscription.exception;

public class SubscriptionAlreadyActiveException extends RuntimeException {
    public SubscriptionAlreadyActiveException(String message) {
        super(message);
    }
} 