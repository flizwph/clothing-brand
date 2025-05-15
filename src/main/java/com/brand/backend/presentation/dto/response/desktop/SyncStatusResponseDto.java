package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для ответа о статусе синхронизации
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusResponseDto {

    /**
     * Временная метка запроса статуса
     */
    private LocalDateTime timestamp;
    
    /**
     * Общий статус синхронизации
     */
    private String overallStatus;
    
    /**
     * Доступность сервера синхронизации
     */
    private boolean serverAvailable;
    
    /**
     * Время последней успешной синхронизации
     */
    private LocalDateTime lastSuccessfulSync;
    
    /**
     * Текущий режим работы (ONLINE, OFFLINE, MIXED)
     */
    private String currentMode;
    
    /**
     * Статус синхронизации по типам данных
     */
    private Map<String, EntitySyncStatus> entityStatus;
    
    /**
     * Активные офлайн-токены
     */
    private List<OfflineToken> activeOfflineTokens;
    
    /**
     * Ограничения для офлайн-режима
     */
    private OfflineRestrictions offlineRestrictions;
    
    /**
     * Класс для описания статуса синхронизации одного типа данных
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntitySyncStatus {
        
        /**
         * Тип данных
         */
        private String entityType;
        
        /**
         * Статус синхронизации
         */
        private String status;
        
        /**
         * Время последней синхронизации
         */
        private LocalDateTime lastSyncTime;
        
        /**
         * Количество локальных объектов
         */
        private Integer localCount;
        
        /**
         * Количество серверных объектов
         */
        private Integer serverCount;
        
        /**
         * Количество объектов, ожидающих синхронизации
         */
        private Integer pendingCount;
        
        /**
         * Процент синхронизации
         */
        private Double syncPercentage;
    }
    
    /**
     * Класс для описания офлайн-токена
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfflineToken {
        
        /**
         * Токен
         */
        private String token;
        
        /**
         * Время создания
         */
        private LocalDateTime createdAt;
        
        /**
         * Время истечения срока действия
         */
        private LocalDateTime expiresAt;
        
        /**
         * Идентификатор устройства
         */
        private String deviceId;
        
        /**
         * Статус
         */
        private String status;
    }
    
    /**
     * Класс для описания ограничений офлайн-режима
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfflineRestrictions {
        
        /**
         * Максимальное время работы в офлайн-режиме (в часах)
         */
        private Integer maxOfflineTime;
        
        /**
         * Ограниченные типы операций
         */
        private List<String> restrictedOperations;
        
        /**
         * Ограниченные типы данных
         */
        private List<String> restrictedEntityTypes;
        
        /**
         * Требуется ли синхронизация при возвращении в онлайн-режим
         */
        private boolean syncRequiredOnReconnect;
    }
} 