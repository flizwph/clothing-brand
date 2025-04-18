package com.brand.backend.application.product.service;

import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(String name, double price, int quantityS, int quantityM, int quantityL) {
        Product product = Product.createProduct(name, price, quantityS, quantityM, quantityL);
        return productRepository.save(product);
    }

    public Product updateProductStock(Long productId, int quantityS, int quantityM, int quantityL) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));
        
        product.setAvailableQuantityS(quantityS);
        product.setAvailableQuantityM(quantityM);
        product.setAvailableQuantityL(quantityL);
        
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }


    public List<Product> getProductsBySize(String size) {
        return productRepository.findBySize(size);
    }
}