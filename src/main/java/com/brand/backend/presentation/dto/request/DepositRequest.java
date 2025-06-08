package com.brand.backend.presentation.dto.request;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO для запроса на пополнение баланса
 */
@Data
public class DepositRequest {
    
    @NotNull(message = "Сумма не может быть пустой")
    @DecimalMin(value = "0.01", message = "Минимальная сумма пополнения 0.01")
    private BigDecimal amount;
} 