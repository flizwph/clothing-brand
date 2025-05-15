package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO для запроса на синхронизацию настроек
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsSyncRequest {

    /**
     * Временная метка последнего обновления настроек на клиенте
     */
    @NotNull(message = "Временная метка последнего обновления обязательна")
    private LocalDateTime lastUpdated;
    
    /**
     * Идентификатор устройства
     */
    @jakarta.validation.constraints.NotBlank(message = "Идентификатор устройства обязателен")
    private String deviceId;
    
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
     * Версия клиентского приложения
     */
    private String appVersion;
    
    /**
     * Флаг сброса настроек к значениям по умолчанию
     */
    private boolean resetToDefaults;
} 