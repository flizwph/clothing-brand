package com.brand.backend.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для создания цифрового заказа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalOrderDto {
    
    @NotEmpty(message = "Список товаров не может быть пустым")
    @Valid
    private List<DigitalOrderItemDto> items;
    
    @NotBlank(message = "Способ оплаты не может быть пустым")
    private String paymentMethod;
    
    private String cryptoAddress;
    private String promoCode;
} 