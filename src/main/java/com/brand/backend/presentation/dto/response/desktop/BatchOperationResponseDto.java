package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO для ответа с результатами пакетного выполнения операций
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResponseDto {

    /**
     * Идентификатор операции
     */
    private String id;
    
    /**
     * Успешное выполнение
     */
    private boolean success;
    
    /**
     * HTTP статус-код ответа
     */
    private int statusCode;
    
    /**
     * Код ошибки (если есть)
     */
    private String errorCode;
    
    /**
     * Сообщение об ошибке (если есть)
     */
    private String errorMessage;
    
    /**
     * Тело ответа
     */
    private Map<String, Object> data;
    
    /**
     * Время начала выполнения
     */
    private LocalDateTime startTime;
    
    /**
     * Время завершения выполнения
     */
    private LocalDateTime endTime;
    
    /**
     * Длительность выполнения в миллисекундах
     */
    private Long executionTimeMs;
} 