package com.brand.backend.presentation.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.brand.backend.domain.order.model.OrderStatus;

@Getter
@Setter
public class OrderResponseDto {
    private Long id;
    private String orderNumber;
    private String productName;
    private String size;
    private int quantity;
    private double price;
    private String telegramUsername;
    private String paymentMethod;
    private String orderComment;
    private LocalDateTime createdAt;
    private OrderStatus status;

    public OrderResponseDto(Long id, String orderNumber, String productName, String size, int quantity, double price,
                            String telegramUsername, String paymentMethod, String orderComment, LocalDateTime createdAt, OrderStatus status) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.productName = productName;
        this.size = size;
        this.quantity = quantity;
        this.price = price;
        this.telegramUsername = telegramUsername;
        this.paymentMethod = paymentMethod;
        this.orderComment = orderComment;
        this.createdAt = createdAt;
        this.status = status;
    }
}
