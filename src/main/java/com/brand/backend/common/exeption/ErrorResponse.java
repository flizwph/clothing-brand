package com.brand.backend.common.exeption;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Стандартизированный ответ с ошибкой для глобального обработчика исключений
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private int status;
    private String message;
    private String path;
    private ZonedDateTime timestamp;
    private String requestId;
    
    @Builder.Default
    private Map<String, Object> additionalInfo = new HashMap<>();
    
    /**
     * Добавляет дополнительную информацию об ошибке
     */
    public void addAdditionalInfo(String key, Object value) {
        if (additionalInfo == null) {
            additionalInfo = new HashMap<>();
        }
        additionalInfo.put(key, value);
    }
} 