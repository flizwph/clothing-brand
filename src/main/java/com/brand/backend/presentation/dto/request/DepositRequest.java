package com.brand.backend.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO для запроса на пополнение баланса
 */
@Data
public class DepositRequest {
    
    /**
     * Сумма для пополнения
     */
    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "1.0", message = "Минимальная сумма пополнения - 1 рубль")
    @Digits(integer = 10, fraction = 2, message = "Некорректный формат суммы")
    private BigDecimal amount;
} 