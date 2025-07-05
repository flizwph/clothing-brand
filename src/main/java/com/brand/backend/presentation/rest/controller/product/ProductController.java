package com.brand.backend.presentation.rest.controller.product;

import com.brand.backend.domain.product.model.Product;
import com.brand.backend.application.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        try {
            // Пытаемся парсить как Long
            Long productId = Long.parseLong(id);
            Optional<Product> product = productService.getProductById(productId);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            // Если не число, возвращаем 400 Bad Request
            return ResponseEntity.badRequest().build();
        }
    }

/*    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        Optional<Product> existingProduct = productService.getProductById(id);
        if (existingProduct.isPresent()) {
            product.setId(id);
            return ResponseEntity.ok(productService.updateProduct(product));
        } else {
            return ResponseEntity.notFound().build();
        }
    }*/

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        try {
            Long productId = Long.parseLong(id);
            Optional<Product> product = productService.getProductById(productId);
        if (product.isPresent()) {
                productService.deleteProduct(productId);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }



    @GetMapping("/size/{size}")
    public ResponseEntity<List<Product>> getProductsBySize(@PathVariable String size) {
        return ResponseEntity.ok(productService.getProductsBySize(size));
    }
}
