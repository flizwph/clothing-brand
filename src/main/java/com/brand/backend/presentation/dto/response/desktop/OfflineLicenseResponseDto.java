package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для ответа с офлайн-токеном лицензии
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineLicenseResponseDto {

    /**
     * Офлайн-токен для проверки лицензии
     */
    private String offlineToken;
    
    /**
     * Дата истечения срока действия офлайн-токена
     */
    private LocalDateTime expirationDate;
    
    /**
     * Количество дней действия токена
     */
    private Integer validDays;
    
    /**
     * Идентификатор устройства
     */
    private String deviceId;
    
    /**
     * Ключ лицензии
     */
    private String licenseKey;
    
    /**
     * Информация о доступных функциях
     */
    private LicenseFeaturesDto features;
} 