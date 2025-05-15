package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO для функций, доступных в лицензии
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseFeaturesDto {

    /**
     * Доступность премиум-функций
     */
    private boolean premiumFeatures;
    
    /**
     * Доступность расширенного редактирования
     */
    private boolean advancedEditing;
    
    /**
     * Доступность импорта данных
     */
    private boolean dataImport;
    
    /**
     * Доступность экспорта данных
     */
    private boolean dataExport;
    
    /**
     * Доступность офлайн-режима
     */
    private boolean offlineMode;
    
    /**
     * Максимальный период офлайн-использования (в днях)
     */
    private Integer maxOfflineDays;
    
    /**
     * Доступ к облачному хранилищу
     */
    private boolean cloudStorage;
    
    /**
     * Размер доступного облачного хранилища (в МБ)
     */
    private Integer cloudStorageSize;
    
    /**
     * Список доступных плагинов
     */
    private List<String> availablePlugins;
    
    /**
     * Карта дополнительных настроек функций
     */
    private Map<String, Object> additionalFeatures;
} 