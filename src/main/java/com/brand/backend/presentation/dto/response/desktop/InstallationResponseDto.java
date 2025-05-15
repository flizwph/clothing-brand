package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для ответа с информацией об установке приложения
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallationResponseDto {

    /**
     * Идентификатор установки
     */
    private Long id;
    
    /**
     * Ключ лицензии
     */
    private String licenseKey;
    
    /**
     * Имя устройства
     */
    private String deviceName;
    
    /**
     * Идентификатор устройства
     */
    private String deviceId;
    
    /**
     * Информация об ОС
     */
    private String osInfo;
    
    /**
     * Дата активации
     */
    private LocalDateTime activationDate;
    
    /**
     * Дата последней активности
     */
    private LocalDateTime lastActivityDate;
    
    /**
     * Статус активности
     */
    private boolean active;
    
    /**
     * Уникальный ключ для активации
     */
    private String activationToken;
    
    /**
     * Полная информация о лицензии
     */
    private LicenseStatusResponseDto licenseInfo;
} 