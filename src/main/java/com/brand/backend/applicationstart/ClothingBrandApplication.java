package com.brand.backend.applicationstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.brand.backend")
@EntityScan({"com.brand.backend.domain"})
@EnableJpaRepositories({"com.brand.backend.domain"})
public class ClothingBrandApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClothingBrandApplication.class, args);
    }
}