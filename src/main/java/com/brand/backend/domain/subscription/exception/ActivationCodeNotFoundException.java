package com.brand.backend.domain.subscription.exception;

public class ActivationCodeNotFoundException extends RuntimeException {
    public ActivationCodeNotFoundException(String message) {
        super(message);
    }
} 