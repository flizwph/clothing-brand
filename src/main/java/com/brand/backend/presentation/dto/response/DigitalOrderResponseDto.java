package com.brand.backend.presentation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO ответа для цифрового заказа
 */
@Data
@Builder
public class DigitalOrderResponseDto {
    private Long id;
    private String orderNumber;
    private Double totalPrice;
    private boolean paid;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime paymentDate;
    private List<DigitalOrderItemResponseDto> items;
} 