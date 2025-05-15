package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO для ответа с настройками кэширования
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachePolicyResponseDto {

    /**
     * Текущие настройки политики кэширования
     */
    private CacheSettings settings;
    
    /**
     * Статистика кэширования
     */
    private CacheStats stats;
    
    /**
     * Дата последнего обновления настроек
     */
    private LocalDateTime lastUpdated;
    
    /**
     * Настройки кэширования
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheSettings {
        
        /**
         * Глобальные настройки
         */
        private GlobalSettings global;
        
        /**
         * Настройки для отдельных ресурсов
         */
        private Map<String, ResourceSettings> resources;
    }
    
    /**
     * Глобальные настройки кэширования
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalSettings {
        
        /**
         * Активация кэширования
         */
        private boolean enabled;
        
        /**
         * Максимальный размер кэша (в МБ)
         */
        private Integer maxCacheSize;
        
        /**
         * Время жизни кэша (в минутах)
         */
        private Integer defaultTtl;
        
        /**
         * Кэширование при низком интернет-соединении
         */
        private boolean cachePoorConnections;
        
        /**
         * Автоматическая очистка кэша при нехватке места
         */
        private boolean autoCleanup;
    }
    
    /**
     * Настройки кэширования для конкретного типа ресурса
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceSettings {
        
        /**
         * Активация кэширования для данного ресурса
         */
        private boolean enabled;
        
        /**
         * Время жизни кэша для данного ресурса (в минутах)
         */
        private Integer ttl;
        
        /**
         * Приоритет (1-10, где 10 - наивысший)
         */
        private Integer priority;
        
        /**
         * Предзагрузка данных при старте приложения
         */
        private boolean preload;
    }
    
    /**
     * Статистика использования кэша
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheStats {
        
        /**
         * Занятое пространство (в МБ)
         */
        private Double usedSpace;
        
        /**
         * Доступное пространство (в МБ)
         */
        private Double availableSpace;
        
        /**
         * Процент использования
         */
        private Double usagePercent;
        
        /**
         * Количество элементов в кэше
         */
        private Integer itemCount;
        
        /**
         * Статистика по отдельным ресурсам
         */
        private Map<String, ResourceStats> resourceStats;
    }
    
    /**
     * Статистика использования кэша для отдельного ресурса
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceStats {
        
        /**
         * Занятое пространство (в МБ)
         */
        private Double usedSpace;
        
        /**
         * Количество элементов
         */
        private Integer itemCount;
        
        /**
         * Процент попаданий в кэш
         */
        private Double hitRate;
        
        /**
         * Дата последнего обновления
         */
        private LocalDateTime lastUpdated;
    }
} 