package com.brand.backend.config;

import com.brand.backend.models.Product;
import com.brand.backend.repositories.ProductRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ProductDataInitializer {
    private final ProductRepository productRepository;

    @PostConstruct
    @Transactional
    public void init() {
        if (productRepository.count() == 0) {
            Product bloodTShirt = Product.createProduct("Blood version", 2999, 100, 100, 100);
            Product BaseTShirt = Product.createProduct("Base edition", 2299, 100, 100, 100);
            productRepository.save(bloodTShirt);
            productRepository.save(BaseTShirt);
        }
    }
}