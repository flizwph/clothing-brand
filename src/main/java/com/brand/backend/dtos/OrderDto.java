package com.brand.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDto {
    private Long productId;
    private String size;
    private String email;
    private String fullName;
    private String country;
    private String address;
    private String postalCode;
    private String phoneNumber;
    private String telegramUsername;
    private String metamaskAddress;
    private String orderComment;
    private String promoCode;
    private String paymentMethod;
}