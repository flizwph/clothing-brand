package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для запроса на синхронизацию данных
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSyncRequest {

    /**
     * Тип данных для синхронизации
     */
    @NotBlank(message = "Тип данных обязателен")
    private String dataType;
    
    /**
     * Временная метка последней синхронизации
     */
    private LocalDateTime lastSync;
    
    /**
     * Список изменений, сделанных в клиенте
     */
    @NotNull(message = "Список изменений обязателен")
    private List<DataChange> changes;
    
    /**
     * Идентификатор устройства
     */
    @NotBlank(message = "Идентификатор устройства обязателен")
    private String deviceId;
    
    /**
     * Идентификатор клиентской сессии
     */
    private String sessionId;
    
    /**
     * Версия клиентского приложения
     */
    private String appVersion;
    
    /**
     * Класс для описания одного изменения
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataChange {
        
        /**
         * Идентификатор изменения
         */
        @NotBlank(message = "Идентификатор изменения обязателен")
        private String id;
        
        /**
         * Тип операции (CREATE, UPDATE, DELETE)
         */
        @NotBlank(message = "Тип операции обязателен")
        private String operation;
        
        /**
         * Временная метка изменения
         */
        @NotNull(message = "Временная метка изменения обязательна")
        private LocalDateTime timestamp;
        
        /**
         * Данные для операции
         */
        private Map<String, Object> data;
        
        /**
         * Идентификатор исходного объекта для UPDATE и DELETE
         */
        private String targetId;
        
        /**
         * Версия объекта для разрешения конфликтов
         */
        private Long version;
        
        /**
         * Флаг конфликта
         */
        private boolean hasConflict;
    }
} 