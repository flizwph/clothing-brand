package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса на отправку оценки приложения
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRatingRequest {

    /**
     * Рейтинг приложения (от 1 до 5)
     */
    @Min(value = 1, message = "Минимальный рейтинг - 1")
    @Max(value = 5, message = "Максимальный рейтинг - 5")
    private Integer rating;
    
    /**
     * Текст отзыва (опционально)
     */
    @Size(max = 1000, message = "Текст отзыва не должен превышать 1000 символов")
    private String comment;
    
    /**
     * Версия приложения
     */
    @Size(max = 50, message = "Версия приложения не должна превышать 50 символов")
    private String appVersion;
    
    /**
     * Время использования приложения (в часах)
     */
    private Integer usageHours;
    
    /**
     * Информация о системе
     */
    @Size(max = 500, message = "Информация о системе не должна превышать 500 символов")
    private String systemInfo;
    
    /**
     * Разрешение на публикацию отзыва
     */
    private boolean allowPublishing;
} 