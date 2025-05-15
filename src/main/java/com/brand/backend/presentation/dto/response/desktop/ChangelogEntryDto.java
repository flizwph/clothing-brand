package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO с информацией об изменениях в версии приложения
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangelogEntryDto {

    /**
     * Версия приложения
     */
    private String version;
    
    /**
     * Дата выпуска версии
     */
    private LocalDateTime releaseDate;
    
    /**
     * Добавленные функции
     */
    private List<String> added;
    
    /**
     * Улучшенные функции
     */
    private List<String> improved;
    
    /**
     * Исправленные ошибки
     */
    private List<String> fixed;
    
    /**
     * Удаленные функции
     */
    private List<String> removed;
    
    /**
     * Известные проблемы
     */
    private List<String> knownIssues;
    
    /**
     * Дополнительные комментарии к версии
     */
    private String additionalNotes;
    
    /**
     * Технические изменения
     */
    private Map<String, List<String>> technical;
} 