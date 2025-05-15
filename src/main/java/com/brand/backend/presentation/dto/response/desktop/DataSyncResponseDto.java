package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для ответа с результатами синхронизации данных
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSyncResponseDto {

    /**
     * Тип данных, которые были синхронизированы
     */
    private String dataType;
    
    /**
     * Временная метка синхронизации на сервере
     */
    private LocalDateTime syncTimestamp;
    
    /**
     * Серверные изменения, которые нужно применить на клиенте
     */
    private List<DataChange> serverChanges;
    
    /**
     * Результаты обработки клиентских изменений
     */
    private List<ChangeResult> changeResults;
    
    /**
     * Статус синхронизации
     */
    private String status;
    
    /**
     * Есть ли конфликты
     */
    private boolean hasConflicts;
    
    /**
     * Рекомендуемое время следующей синхронизации
     */
    private LocalDateTime nextSyncRecommended;
    
    /**
     * Общее количество объектов данного типа
     */
    private Integer totalCount;
    
    /**
     * Класс для описания изменения от сервера
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataChange {
        
        /**
         * Идентификатор изменения
         */
        private String id;
        
        /**
         * Тип операции (CREATE, UPDATE, DELETE)
         */
        private String operation;
        
        /**
         * Временная метка изменения
         */
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
         * Версия объекта
         */
        private Long version;
    }
    
    /**
     * Класс для описания результата обработки клиентского изменения
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeResult {
        
        /**
         * Идентификатор изменения
         */
        private String id;
        
        /**
         * Статус обработки (SUCCESS, CONFLICT, ERROR)
         */
        private String status;
        
        /**
         * Сообщение об ошибке (если есть)
         */
        private String errorMessage;
        
        /**
         * Конфликтующие данные (если есть)
         */
        private Map<String, Object> conflictData;
        
        /**
         * Версия объекта после применения изменений
         */
        private Long newVersion;
        
        /**
         * Идентификатор объекта после применения изменений (для CREATE)
         */
        private String newId;
    }
} 