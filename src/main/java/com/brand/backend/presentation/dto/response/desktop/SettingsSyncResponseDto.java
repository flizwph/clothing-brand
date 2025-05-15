package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO для ответа с результатами синхронизации настроек
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsSyncResponseDto {

    /**
     * Временная метка синхронизации на сервере
     */
    private LocalDateTime syncTimestamp;
    
    /**
     * Настройки приложения
     */
    private Map<String, Object> appSettings;
    
    /**
     * Настройки интерфейса
     */
    private Map<String, Object> uiSettings;
    
    /**
     * Настройки уведомлений
     */
    private Map<String, Object> notificationSettings;
    
    /**
     * Настройки синхронизации
     */
    private Map<String, Object> syncSettings;
    
    /**
     * Другие настройки
     */
    private Map<String, Object> otherSettings;
    
    /**
     * Статус синхронизации
     */
    private String status;
    
    /**
     * Настройки, которые были объединены (имели конфликты)
     */
    private Map<String, Object> mergedSettings;
    
    /**
     * Настройки, специфичные для данного устройства
     */
    private Map<String, Object> deviceSpecificSettings;
    
    /**
     * Рекомендации по настройкам
     */
    private Map<String, Object> recommendedSettings;
} 