package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO для запроса на управление кэшированием данных
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachePolicyRequest {

    /**
     * Глобальные настройки кэширования
     */
    @NotNull(message = "Глобальные настройки кэширования обязательны")
    @Valid
    private GlobalCacheSettings globalSettings;
    
    /**
     * Настройки кэширования для отдельных типов данных
     */
    private Map<String, @Valid ResourceCacheSettings> resourceSettings;
    
    /**
     * Глобальные настройки кэширования
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalCacheSettings {
        
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
    public static class ResourceCacheSettings {
        
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
        
        /**
         * Исключения из кэширования (регулярные выражения)
         */
        private List<String> exclusions;
    }
} 