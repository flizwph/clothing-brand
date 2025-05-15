package com.brand.backend.application.security.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Модель для оповещений безопасности
 */
@Data
@Builder
public class SecurityAlert {
    
    /**
     * Время создания алерта
     */
    private LocalDateTime timestamp;
    
    /**
     * Тип алерта
     */
    private AlertType type;
    
    /**
     * Сообщение алерта
     */
    private String message;
    
    /**
     * Дополнительные детали для алерта
     */
    private Map<String, Object> details;
} 