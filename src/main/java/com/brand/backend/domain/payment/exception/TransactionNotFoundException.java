package com.brand.backend.domain.payment.exception;

/**
 * Исключение, выбрасываемое когда транзакция не найдена
 */
public class TransactionNotFoundException extends TransactionException {
    
    public TransactionNotFoundException(String transactionCode) {
        super("Транзакция с кодом " + transactionCode + " не найдена");
    }
    
    public TransactionNotFoundException(Long transactionId) {
        super("Транзакция с ID " + transactionId + " не найдена");
    }
} 