package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO для ответа с информацией об обновлении приложения
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUpdateResponseDto {

    /**
     * Версия обновления
     */
    private String version;
    
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
     * Тип обновления (FULL, PATCH, DELTA)
     */
    private String updateType;
    
    /**
     * Требуется ли перезапуск приложения после обновления
     */
    private boolean requiresRestart;
    
    /**
     * Дата выпуска обновления
     */
    private LocalDateTime releaseDate;
    
    /**
     * Список изменений в новой версии
     */
    private ChangelogEntryDto changelog;
    
    /**
     * Инструкции по установке (если требуются)
     */
    private String installationInstructions;
    
    /**
     * Минимальные системные требования
     */
    private Map<String, String> systemRequirements;
} 