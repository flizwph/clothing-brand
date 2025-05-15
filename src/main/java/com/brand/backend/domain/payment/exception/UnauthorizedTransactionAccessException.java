package com.brand.backend.domain.payment.exception;

/**
 * Исключение, выбрасываемое при попытке получить доступ к чужой транзакции
 */
public class UnauthorizedTransactionAccessException extends TransactionException {
    
    public UnauthorizedTransactionAccessException() {
        super("Доступ к данной транзакции запрещен");
    }
    
    public UnauthorizedTransactionAccessException(String username, String transactionCode) {
        super("Пользователь " + username + " пытается получить доступ к чужой транзакции: " + transactionCode);
    }
} 