package com.brand.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Конфигурация для поддержки работы приложения за балансировщиком нагрузки
 */
@Configuration
public class LoadBalancerConfig {
    
    /**
     * Фильтр для корректной обработки заголовков X-Forwarded-*
     * Позволяет приложению правильно определять IP-адреса клиентов и формировать URL
     * при работе за прокси-сервером или балансировщиком нагрузки
     */
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
} 