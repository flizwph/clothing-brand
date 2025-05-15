package com.brand.backend.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO с инструкциями для пополнения баланса
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositInstructionsDto {
    
    private Long transactionId;
    private BigDecimal amount;
    private String transactionCode;
    private String cardNumber;
    private String cardholderName;
    private String bankName;
    private String message;
} 