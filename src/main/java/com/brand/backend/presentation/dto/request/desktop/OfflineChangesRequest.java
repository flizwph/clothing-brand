package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для запроса на синхронизацию офлайн-изменений
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineChangesRequest {

    /**
     * Идентификатор устройства
     */
    @NotBlank(message = "Идентификатор устройства обязателен")
    private String deviceId;
    
    /**
     * Временная метка начала офлайн-работы
     */
    private LocalDateTime offlineStartTime;
    
    /**
     * Временная метка окончания офлайн-работы
     */
    private LocalDateTime offlineEndTime;
    
    /**
     * Список офлайн-изменений
     */
    @NotEmpty(message = "Список изменений не может быть пустым")
    private List<OfflineChange> changes;
    
    /**
     * Офлайн-токен безопасности
     */
    @NotBlank(message = "Офлайн-токен безопасности обязателен")
    private String offlineToken;
    
    /**
     * Класс для описания одного офлайн-изменения
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfflineChange {
        
        /**
         * Уникальный идентификатор изменения
         */
        @NotBlank(message = "Идентификатор изменения обязателен")
        private String id;
        
        /**
         * Тип данных (products, settings, etc.)
         */
        @NotBlank(message = "Тип данных обязателен")
        private String entityType;
        
        /**
         * Тип операции (CREATE, UPDATE, DELETE)
         */
        @NotBlank(message = "Тип операции обязателен")
        private String operation;
        
        /**
         * Временная метка изменения
         */
        private LocalDateTime timestamp;
        
        /**
         * Данные изменения
         */
        private Map<String, Object> data;
        
        /**
         * Идентификатор объекта для UPDATE и DELETE
         */
        private String entityId;
        
        /**
         * Приоритет изменения (для разрешения конфликтов)
         */
        private Integer priority;
    }
} 