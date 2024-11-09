package com.brand.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDto {
    private Long id;             // Уникальный идентификатор продукта
    private String name;         // Название продукта
    private double price;        // Цена продукта
    private String size;         // Размер продукта (S, M, L)
    private int availableQuantity; // Доступное количество товара в выбранном размере

    public ProductDto(Long id, String name, double price, String size, int availableQuantity) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.size = size;
        this.availableQuantity = availableQuantity;
    }
}
