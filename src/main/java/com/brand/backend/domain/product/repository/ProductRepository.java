package com.brand.backend.domain.product.repository;

import com.brand.backend.domain.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE :size MEMBER OF p.sizes")
    List<Product> findBySize(String size);

}