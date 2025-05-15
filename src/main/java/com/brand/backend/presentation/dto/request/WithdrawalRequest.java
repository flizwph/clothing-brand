package com.brand.backend.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO для запроса на вывод средств с баланса
 */
@Data
public class WithdrawalRequest {
    
    /**
     * Сумма для вывода
     */
    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "1.0", message = "Минимальная сумма вывода - 1 рубль")
    @Digits(integer = 10, fraction = 2, message = "Некорректный формат суммы")
    private BigDecimal amount;
    
    /**
     * Реквизиты для вывода (номер карты или другие платежные данные)
     */
    @NotBlank(message = "Необходимо указать реквизиты для вывода средств")
    private String paymentDetails;
    
    /**
     * Комментарий к выводу (опционально)
     */
    private String comment;
} 