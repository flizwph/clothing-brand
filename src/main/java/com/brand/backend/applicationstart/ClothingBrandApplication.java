package com.brand.backend.applicationstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.brand.backend")
@EnableJpaRepositories(basePackages = {
    "com.brand.backend.domain.user.repository",
    "com.brand.backend.domain.product.repository", 
    "com.brand.backend.domain.order.repository",
    "com.brand.backend.domain.payment.repository",
    "com.brand.backend.domain.balance.repository",
    "com.brand.backend.domain.nft.repository",
    "com.brand.backend.domain.subscription.repository",
    "com.brand.backend.domain.crypto.repository",
    "com.brand.backend.domain.activity.repository",
    "com.brand.backend.domain.promotion.repository",
    "com.brand.backend.infrastructure.security.audit",
    "com.brand.backend.infrastructure.persistence.repository"
})
@EntityScan(basePackages = {
    "com.brand.backend.domain.user.model",
    "com.brand.backend.domain.product.model",
    "com.brand.backend.domain.order.model", 
    "com.brand.backend.domain.payment.model",
    "com.brand.backend.domain.balance.model",
    "com.brand.backend.domain.nft.model",
    "com.brand.backend.domain.subscription.model",
    "com.brand.backend.domain.crypto.model",
    "com.brand.backend.domain.activity.model",
    "com.brand.backend.domain.promotion.model",
    "com.brand.backend.infrastructure.security.audit",
    "com.brand.backend.infrastructure.persistence.entity"
})
@EnableAsync
@EnableScheduling
public class ClothingBrandApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClothingBrandApplication.class, args);
    }
}