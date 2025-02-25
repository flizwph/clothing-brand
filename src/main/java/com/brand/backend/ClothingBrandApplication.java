package com.brand.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EntityScan("com.brand.backend.models")
@SpringBootApplication
public class ClothingBrandApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClothingBrandApplication.class, args);
    }

}
