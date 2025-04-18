package com.brand.backend.infrastructure.config;

import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.product.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ProductDataInitializer {
    private final ProductRepository productRepository;

    @PostConstruct
    @Transactional
    public void init() {
        if (productRepository.count() == 0) {
            Product bloodTShirt = Product.createProduct("RC INCIDENTS", 2999, 100, 100, 100);
            bloodTShirt.setSizes(List.of("M", "L", "XL"));

/*            Product baseTShirt = Product.createProduct("Base edition", 2299, 100, 100, 100);
            baseTShirt.setSizes(List.of("S", "M", "L"));*/

            productRepository.save(bloodTShirt);
//            productRepository.save(baseTShirt);

        }
    }
}