package com.brand.backend.presentation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO для инструкций по пополнению баланса
 */
@Data
@Builder
public class DepositInstructionsDto {
    private String transactionCode;    // Код транзакции
    private BigDecimal amount;         // Сумма
    private String currency;           // Валюта
    private String instructions;       // Инструкции для пользователя
    private String message;            // Сообщение о статусе
} 