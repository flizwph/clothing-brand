package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса валидации лицензии
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseValidationRequest {

    /**
     * Лицензионный ключ
     */
    @NotBlank(message = "Лицензионный ключ обязателен")
    @Size(min = 16, max = 64, message = "Длина лицензионного ключа должна быть от 16 до 64 символов")
    private String licenseKey;

    /**
     * Идентификатор устройства (hardware ID)
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
     * Версия приложения
     */
    @Size(max = 50, message = "Версия приложения не должна превышать 50 символов")
    private String appVersion;
} 