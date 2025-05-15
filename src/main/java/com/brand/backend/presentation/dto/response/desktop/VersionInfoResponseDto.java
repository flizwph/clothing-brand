package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для ответа с информацией о версии приложения
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionInfoResponseDto {

    /**
     * Последняя доступная версия
     */
    private String latestVersion;
    
    /**
     * Дата выпуска последней версии
     */
    private LocalDateTime releaseDate;
    
    /**
     * Наличие обязательного обновления
     */
    private boolean mandatoryUpdate;
    
    /**
     * Минимальная версия, для которой доступно прямое обновление
     */
    private String minVersionForUpdate;
    
    /**
     * Наличие обновления для текущей версии
     */
    private boolean updateAvailable;
    
    /**
     * Список изменений в новой версии
     */
    private List<String> changelog;
    
    /**
     * URL для скачивания обновления
     */
    private String downloadUrl;
    
    /**
     * Размер файла обновления в байтах
     */
    private Long downloadSize;
    
    /**
     * Хэш-сумма файла обновления (для проверки целостности)
     */
    private String downloadHash;
    
    /**
     * Дополнительная информация о версии
     */
    private Map<String, Object> additionalInfo;
} 