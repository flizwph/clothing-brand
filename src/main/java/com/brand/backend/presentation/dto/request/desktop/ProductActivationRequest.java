package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса на активацию цифрового продукта
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductActivationRequest {

    /**
     * Идентификатор устройства
     */
    @NotBlank(message = "Идентификатор устройства обязателен")
    @Size(max = 128, message = "Идентификатор устройства не должен превышать 128 символов")
    private String deviceId;
    
    /**
     * Ключ безопасности, сгенерированный на устройстве
     */
    @NotBlank(message = "Ключ безопасности обязателен")
    @Size(max = 256, message = "Ключ безопасности не должен превышать 256 символов")
    private String securityKey;
    
    /**
     * Имя устройства
     */
    @Size(max = 100, message = "Имя устройства не должно превышать 100 символов")
    private String deviceName;
    
    /**
     * Информация об операционной системе
     */
    @Size(max = 100, message = "Информация об ОС не должна превышать 100 символов")
    private String osInfo;
    
    /**
     * Версия приложения
     */
    @Size(max = 50, message = "Версия приложения не должна превышать 50 символов")
    private String appVersion;
} 