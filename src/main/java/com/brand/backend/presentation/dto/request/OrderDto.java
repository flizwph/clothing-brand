package com.brand.backend.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    
    @NotNull(message = "ID товара не может быть пустым")
    private Long productId;
    
    @NotNull(message = "Укажите количество")
    @Min(value = 1, message = "Количество не может быть меньше 1")
    private Integer quantity;
    
    @NotBlank(message = "Размер не может быть пустым")
    @Pattern(regexp = "^[SMLsml]$|^(XL|xl)$", message = "Укажите корректный размер (S, M, L, XL)")
    private String size;
    
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Введите корректный email")
    private String email;
    
    @NotBlank(message = "ФИО не может быть пустым")
    @Size(min = 3, max = 100, message = "ФИО должно быть от 3 до 100 символов")
    private String fullName;
    
    @NotBlank(message = "Страна не может быть пустой")
    private String country;
    
    @NotBlank(message = "Адрес не может быть пустым")
    @Size(min = 5, max = 200, message = "Адрес должен быть от 5 до 200 символов")
    private String address;
    
    @NotBlank(message = "Почтовый индекс не может быть пустым")
    @Pattern(regexp = "^\\d{5,10}$", message = "Укажите корректный почтовый индекс")
    private String postalCode;
    
    @NotBlank(message = "Номер телефона не может быть пустым")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Введите корректный номер телефона")
    private String phoneNumber;
    
    private String telegramUsername;
    private String cryptoAddress;
    private String orderComment;
    private String promoCode;
    
    @NotBlank(message = "Способ оплаты не может быть пустым")
    private String paymentMethod;
}
