package com.brand.backend.infrastructure.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                    // Продакшен домены
                    "https://escap1sm.com",
                    "http://escap1sm.com",
                    // React development server (по умолчанию порт 3000)
                    "http://localhost:3000",
                    "https://localhost:3000",
                    // Дополнительные порты для React (если нужно)
                    "http://localhost:3001",
                    "http://localhost:3002",
                    // Existing развивающиеся серверы
                    "https://127.0.0.1:5500",
                    "http://127.0.0.1:5501",
                    "http://127.0.0.1:5502"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Кэширование preflight запросов на 1 час
    }
}
