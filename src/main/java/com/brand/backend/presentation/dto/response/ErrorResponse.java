package com.brand.backend.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Стандартизированный ответ с ошибкой
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * Время возникновения ошибки
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP-статус ошибки
     */
    private int status;
    
    /**
     * Текстовое описание HTTP-статуса
     */
    private String error;
    
    /**
     * Сообщение об ошибке
     */
    private String message;
    
    /**
     * Код ошибки для клиентов
     */
    private String code;
    
    /**
     * Путь к ресурсу, вызвавшему ошибку
     */
    private String path;
    
    /**
     * Уникальный идентификатор ошибки для отслеживания в логах
     */
    private String errorId;
    
    /**
     * Ошибки валидации (поле -> сообщение об ошибке)
     */
    private Map<String, String> validationErrors;
    
    /**
     * Дополнительная информация об ошибке
     */
    @Builder.Default
    private Map<String, Object> additionalInfo = new HashMap<>();
    
    /**
     * Добавляет дополнительную информацию об ошибке
     */
    public void addAdditionalInfo(String key, Object value) {
        additionalInfo.put(key, value);
    }
} 