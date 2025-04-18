package com.brand.backend.domain.order.model;
import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.user.model.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "size", nullable = false)
    private String size;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "crypto_address")
    private String cryptoAddress;

    @Column(name = "order_comment")
    private String orderComment;

    @Column(name = "promo_code")
    private String promoCode;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.NEW;
}
