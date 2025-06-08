package com.brand.backend.domain.payment.exception;

import com.brand.backend.domain.payment.model.TransactionStatus;

public class InvalidTransactionStateException extends RuntimeException {
    
    public InvalidTransactionStateException(TransactionStatus currentStatus, TransactionStatus expectedStatus) {
        super("Невозможно выполнить операцию для транзакции в состоянии " + 
                currentStatus + ". Ожидаемое состояние: " + expectedStatus);
    }
    
    public InvalidTransactionStateException(TransactionStatus currentStatus, String operation) {
        super("Невозможно выполнить операцию '" + operation + "' для транзакции в состоянии " + currentStatus);
    }
}