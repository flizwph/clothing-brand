package com.brand.backend.common.exeption;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ActivationCodeNotFoundException extends RuntimeException {
    public ActivationCodeNotFoundException(String message) {
        super(message);
    }
} 