package com.brand.backend.presentation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO ответа для позиции цифрового заказа
 */
@Data
@Builder
public class DigitalOrderItemResponseDto {
    private Long id;
    private Long digitalProductId;
    private String productName;
    private Integer quantity;
    private Double price;
    private String activationCode;
    private LocalDateTime activationDate;
    private LocalDateTime expirationDate;
    private boolean active;
} 