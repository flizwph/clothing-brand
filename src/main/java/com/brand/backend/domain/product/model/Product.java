package com.brand.backend.domain.product.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "image_url")
    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size")
    private List<String> sizes;

    @Column(name = "available_quantity_s", nullable = false)
    private int availableQuantityS;

    @Column(name = "available_quantity_m", nullable = false)
    private int availableQuantityM;

    @Column(name = "available_quantity_l", nullable = false)
    private int availableQuantityL;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    public static Product createProduct(String name, double price, int quantityS, int quantityM, int quantityL) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setSizes(List.of("S", "M", "L"));
        product.setAvailableQuantityS(quantityS);
        product.setAvailableQuantityM(quantityM);
        product.setAvailableQuantityL(quantityL);
        product.setTotalQuantity(quantityS + quantityM + quantityL);
        return product;
    }

    public static Product createProduct(String name, double price, String imageUrl, int quantityS, int quantityM, int quantityL) {
        Product product = createProduct(name, price, quantityS, quantityM, quantityL);
        product.setImageUrl(imageUrl);
        return product;
    }

    // Методы для работы с общим количеством
    public int getTotalQuantity() {
        if (totalQuantity == null) {
            return this.availableQuantityS + this.availableQuantityM + this.availableQuantityL;
        }
        return totalQuantity;
    }
    
    public void updateTotalQuantity() {
        this.totalQuantity = this.availableQuantityS + this.availableQuantityM + this.availableQuantityL;
    }

    public void setQuantities(int quantityS, int quantityM, int quantityL) {
        this.availableQuantityS = quantityS;
        this.availableQuantityM = quantityM;
        this.availableQuantityL = quantityL;
        updateTotalQuantity();
    }
}