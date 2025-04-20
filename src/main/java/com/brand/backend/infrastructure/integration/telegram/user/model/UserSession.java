package com.brand.backend.infrastructure.integration.telegram.user.model;

import com.brand.backend.domain.product.model.Product;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class UserSession {
    private final Long chatId;
    private UserState state;
    private Product selectedProduct;
    private int quantity;
    private String name;
    private String phone;
    private String address;
    private final LocalDateTime createdAt;
    private final Map<String, Object> attributes;

    public UserSession(Long chatId) {
        this.chatId = chatId;
        this.state = UserState.NONE;
        this.createdAt = LocalDateTime.now();
        this.attributes = new HashMap<>();
    }

    public Long getChatId() {
        return chatId;
    }

    public UserState getState() {
        return state;
    }

    public void setState(UserState state) {
        this.state = state;
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product selectedProduct) {
        this.selectedProduct = selectedProduct;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(createdAt.plusHours(1));
    }
} 