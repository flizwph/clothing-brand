package com.brand.backend.domain.payment.exception;

/**
 * Базовое исключение для операций с транзакциями
 */
public class TransactionException extends RuntimeException {
    
    public TransactionException(String message) {
        super(message);
    }
    
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
} 