package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для ответа с информацией об отзыве на продукт
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewResponseDto {

    /**
     * Идентификатор отзыва
     */
    private Long id;
    
    /**
     * Идентификатор продукта
     */
    private Long productId;
    
    /**
     * Название продукта
     */
    private String productName;
    
    /**
     * Имя пользователя (автора отзыва)
     */
    private String username;
    
    /**
     * Рейтинг продукта (от 1 до 5)
     */
    private Integer rating;
    
    /**
     * Заголовок отзыва
     */
    private String title;
    
    /**
     * Текст отзыва
     */
    private String comment;
    
    /**
     * Версия продукта
     */
    private String productVersion;
    
    /**
     * Дата создания отзыва
     */
    private LocalDateTime createdAt;
    
    /**
     * Количество отметок "полезный отзыв"
     */
    private Integer helpfulCount;
    
    /**
     * Ответ от разработчика (если есть)
     */
    private String developerResponse;
    
    /**
     * Дата ответа разработчика
     */
    private LocalDateTime developerResponseDate;
} 