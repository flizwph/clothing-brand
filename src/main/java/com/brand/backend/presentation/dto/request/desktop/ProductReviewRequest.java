package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса на отправку отзыва о продукте
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewRequest {

    /**
     * Рейтинг продукта (от 1 до 5)
     */
    @Min(value = 1, message = "Минимальный рейтинг - 1")
    @Max(value = 5, message = "Максимальный рейтинг - 5")
    private Integer rating;
    
    /**
     * Заголовок отзыва
     */
    @Size(max = 100, message = "Заголовок отзыва не должен превышать 100 символов")
    private String title;
    
    /**
     * Текст отзыва
     */
    @Size(max = 2000, message = "Текст отзыва не должен превышать 2000 символов")
    private String comment;
    
    /**
     * Версия продукта
     */
    @Size(max = 50, message = "Версия продукта не должна превышать 50 символов")
    private String productVersion;
    
    /**
     * Разрешение на публикацию отзыва
     */
    private boolean allowPublishing;
} 