package com.brand.backend.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для элемента цифрового заказа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalOrderItemDto {
    
    @NotNull(message = "ID цифрового продукта не может быть пустым")
    private Long digitalProductId;
    
    @NotNull(message = "Укажите количество")
    @Min(value = 1, message = "Количество не может быть меньше 1")
    private Integer quantity;
} 