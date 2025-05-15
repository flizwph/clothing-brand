package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для ответа о статусе лицензии
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseStatusResponseDto {

    /**
     * Статус действительности лицензии
     */
    private boolean valid;
    
    /**
     * Тип лицензии (TRIAL, STANDARD, PROFESSIONAL, ENTERPRISE)
     */
    private String licenseType;
    
    /**
     * Дата активации лицензии
     */
    private LocalDateTime activationDate;
    
    /**
     * Дата истечения срока действия лицензии (для временных лицензий)
     */
    private LocalDateTime expirationDate;
    
    /**
     * Количество активных установок
     */
    private Integer activeInstallations;
    
    /**
     * Максимальное количество разрешенных установок
     */
    private Integer maxInstallations;
    
    /**
     * Владелец лицензии (email или имя пользователя)
     */
    private String licensedTo;
    
    /**
     * Доступные функции для данной лицензии
     */
    private LicenseFeaturesDto features;
    
    /**
     * Причина недействительности лицензии (если есть)
     */
    private String invalidReason;
    
    /**
     * Токен обновления для лицензии
     */
    private String refreshToken;
    
    /**
     * Рекомендуемая дата следующей валидации
     */
    private LocalDateTime nextValidationDate;
} 