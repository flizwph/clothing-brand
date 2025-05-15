package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для ответа об оценке приложения
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRatingResponseDto {

    /**
     * Идентификатор оценки
     */
    private Long id;
    
    /**
     * Рейтинг
     */
    private Integer rating;
    
    /**
     * Статус обработки
     */
    private String status;
    
    /**
     * Дата создания
     */
    private LocalDateTime createdAt;
    
    /**
     * Благодарственное сообщение
     */
    private String thankYouMessage;
    
    /**
     * Флаг публикации
     */
    private boolean published;
    
    /**
     * Предложение оставить отзыв в внешнем сервисе
     */
    private boolean promptForExternalReview;
    
    /**
     * Ссылка для внешнего отзыва
     */
    private String externalReviewUrl;
} 