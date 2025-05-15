package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса активации лицензии
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseActivationRequest {

    /**
     * Лицензионный ключ
     */
    @NotBlank(message = "Лицензионный ключ обязателен")
    @Size(min = 16, max = 64, message = "Длина лицензионного ключа должна быть от 16 до 64 символов")
    private String licenseKey;

    /**
     * Имя устройства
     */
    @NotBlank(message = "Имя устройства обязательно")
    @Size(max = 100, message = "Имя устройства не должно превышать 100 символов")
    private String deviceName;

    /**
     * Идентификатор устройства (hardware ID)
     */
    @NotBlank(message = "Идентификатор устройства обязателен")
    @Size(max = 128, message = "Идентификатор устройства не должен превышать 128 символов")
    private String deviceId;

    /**
     * Информация об операционной системе
     */
    @Size(max = 100, message = "Информация об ОС не должна превышать 100 символов")
    private String osInfo;

    /**
     * Ключ безопасности, сгенерированный на устройстве
     */
    @NotBlank(message = "Ключ безопасности обязателен")
    @Size(max = 256, message = "Ключ безопасности не должен превышать 256 символов")
    private String securityKey;
} 