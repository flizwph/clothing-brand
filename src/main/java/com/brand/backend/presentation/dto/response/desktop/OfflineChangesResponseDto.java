package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для ответа с результатами синхронизации офлайн-изменений
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineChangesResponseDto {

    /**
     * Временная метка синхронизации на сервере
     */
    private LocalDateTime syncTimestamp;
    
    /**
     * Результаты обработки офлайн-изменений
     */
    private List<ChangeResult> results;
    
    /**
     * Количество успешно примененных изменений
     */
    private Integer successCount;
    
    /**
     * Количество конфликтов
     */
    private Integer conflictCount;
    
    /**
     * Количество ошибок
     */
    private Integer errorCount;
    
    /**
     * Статус обработки офлайн-изменений
     */
    private String status;
    
    /**
     * Новый офлайн-токен
     */
    private String newOfflineToken;
    
    /**
     * Время действия нового офлайн-токена
     */
    private LocalDateTime newTokenExpirationTime;
    
    /**
     * Класс для описания результата обработки одного изменения
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
         * Статус обработки (SUCCESS, CONFLICT, ERROR, IGNORED)
         */
        private String status;
        
        /**
         * Сообщение об ошибке или причина конфликта
         */
        private String message;
        
        /**
         * Серверная версия данных (при конфликте)
         */
        private Map<String, Object> serverData;
        
        /**
         * Разрешен ли конфликт автоматически
         */
        private Boolean autoResolved;
        
        /**
         * Тип данных
         */
        private String entityType;
        
        /**
         * Идентификатор объекта
         */
        private String entityId;
        
        /**
         * Новая версия объекта после применения изменений
         */
        private Long newVersion;
    }
} 