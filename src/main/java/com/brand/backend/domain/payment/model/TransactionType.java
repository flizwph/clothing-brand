package com.brand.backend.domain.payment.model;

/**
 * Типы транзакций для операций с балансом пользователя
 */
public enum TransactionType {
    /**
     * Пополнение баланса
     */
    DEPOSIT,
    
    /**
     * Снятие средств с баланса
     */
    WITHDRAWAL,
    
    /**
     * Оплата заказа
     */
    ORDER_PAYMENT
} 