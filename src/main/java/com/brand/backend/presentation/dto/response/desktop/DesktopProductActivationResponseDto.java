package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO для ответа с информацией об активации цифрового продукта
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesktopProductActivationResponseDto {

    /**
     * Идентификатор продукта
     */
    private Long productId;
    
    /**
     * Название продукта
     */
    private String productName;
    
    /**
     * Код активации
     */
    private String activationCode;
    
    /**
     * Статус активации (SUCCESS, ALREADY_ACTIVATED, FAILED)
     */
    private String status;
    
    /**
     * Сообщение о результате активации
     */
    private String message;
    
    /**
     * Идентификатор устройства
     */
    private String deviceId;
    
    /**
     * Время активации
     */
    private LocalDateTime activationTime;
    
    /**
     * Время истечения срока действия (если ограничено)
     */
    private LocalDateTime expirationTime;
    
    /**
     * Ключ активации для использования в приложении
     */
    private String activationKey;
    
    /**
     * Детали продукта, доступные после активации
     */
    private Map<String, Object> productDetails;
    
    /**
     * Разблокированные функции
     */
    private Map<String, Boolean> unlockedFeatures;
} 