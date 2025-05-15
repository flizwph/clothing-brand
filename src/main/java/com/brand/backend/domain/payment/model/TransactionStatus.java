package com.brand.backend.domain.payment.model;

/**
 * Статусы транзакций для отслеживания их жизненного цикла
 */
public enum TransactionStatus {
    /**
     * Транзакция создана
     */
    CREATED,
    
    /**
     * Ожидает подтверждения
     */
    PENDING,
    
    /**
     * Успешно завершена
     */
    COMPLETED,
    
    /**
     * Отменена пользователем
     */
    CANCELLED,
    
    /**
     * Отклонена администратором
     */
    REJECTED
} 