package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для ответа об обработке обратной связи
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponseDto {

    /**
     * Идентификатор обратной связи
     */
    private Long id;
    
    /**
     * Тип обратной связи
     */
    private String type;
    
    /**
     * Заголовок обратной связи
     */
    private String subject;
    
    /**
     * Статус обработки
     */
    private String status;
    
    /**
     * Дата создания
     */
    private LocalDateTime createdAt;
    
    /**
     * Уникальный идентификатор для отслеживания
     */
    private String trackingId;
    
    /**
     * Ссылка для отслеживания статуса обработки
     */
    private String trackingUrl;
    
    /**
     * Расчетное время ответа
     */
    private LocalDateTime estimatedResponseTime;
} 