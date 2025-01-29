package com.brand.backend.models;

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

    @Column(name = "price", nullable = false)
    private double price;

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

    public static Product createProduct(String name, double price, int quantityS, int quantityM, int quantityL) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setSizes(List.of("S", "M", "L"));
        product.setAvailableQuantityS(quantityS);
        product.setAvailableQuantityM(quantityM);
        product.setAvailableQuantityL(quantityL);
        return product;
    }
}