package com.brand.backend.services;

import com.brand.backend.models.Product;
import com.brand.backend.repositories.ProductRepository;
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


    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }


    public List<Product> getProductsBySize(String size) {
        return productRepository.findBySize(size);
    }
}