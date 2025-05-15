package com.brand.backend.application.desktop.service;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.DataSyncRequest;
import com.brand.backend.presentation.dto.request.desktop.OfflineChangesRequest;
import com.brand.backend.presentation.dto.request.desktop.SettingsSyncRequest;
import com.brand.backend.presentation.dto.response.desktop.DataSyncResponseDto;
import com.brand.backend.presentation.dto.response.desktop.OfflineChangesResponseDto;
import com.brand.backend.presentation.dto.response.desktop.SettingsSyncResponseDto;
import com.brand.backend.presentation.dto.response.desktop.SyncStatusResponseDto;

import java.time.LocalDateTime;

/**
 * Сервис для синхронизации данных в десктопном приложении
 */
public interface DesktopSyncService {

    /**
     * Получает настройки пользователя для синхронизации
     * 
     * @param user пользователь
     * @return настройки пользователя
     */
    SettingsSyncResponseDto getUserSettings(User user);
    
    /**
     * Обновляет настройки пользователя
     * 
     * @param request данные для обновления настроек
     * @param user пользователь
     * @return результат обновления настроек
     */
    SettingsSyncResponseDto updateUserSettings(SettingsSyncRequest request, User user);
    
    /**
     * Получает пользовательские данные для синхронизации
     * 
     * @param user пользователь
     * @param lastSync временная метка последней синхронизации (опционально)
     * @param dataType тип данных для синхронизации (опционально)
     * @return данные пользователя
     */
    DataSyncResponseDto getUserData(User user, LocalDateTime lastSync, String dataType);
    
    /**
     * Синхронизирует пользовательские данные
     * 
     * @param request данные для синхронизации
     * @param user пользователь
     * @return результат синхронизации
     */
    DataSyncResponseDto syncUserData(DataSyncRequest request, User user);
    
    /**
     * Получает статус синхронизации
     * 
     * @param user пользователь
     * @return статус синхронизации
     */
    SyncStatusResponseDto getSyncStatus(User user);
    
    /**
     * Синхронизирует офлайн-изменения
     * 
     * @param request данные для синхронизации офлайн-изменений
     * @param user пользователь
     * @return результат синхронизации
     */
    OfflineChangesResponseDto syncOfflineChanges(OfflineChangesRequest request, User user);
} 