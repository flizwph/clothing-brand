package com.brand.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для хранения платежных реквизитов
 */
@Configuration
@ConfigurationProperties(prefix = "payment")
@Data
public class PaymentProperties {
    
    /**
     * Номер карты для перевода
     */
    private String cardNumber;
    
    /**
     * Имя владельца карты
     */
    private String cardholderName;
    
    /**
     * Название банка
     */
    private String bankName;
} 