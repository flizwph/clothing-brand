package com.brand.backend.application.desktop.service;

import com.brand.backend.presentation.dto.response.desktop.AppUpdateResponseDto;
import com.brand.backend.presentation.dto.response.desktop.ChangelogEntryDto;
import com.brand.backend.presentation.dto.response.desktop.VersionInfoResponseDto;

import java.util.List;

/**
 * Сервис для управления обновлениями десктопного приложения
 */
public interface DesktopUpdateService {

    /**
     * Получает информацию о последней версии приложения
     * 
     * @param currentVersion текущая версия приложения пользователя
     * @param platform платформа (Windows, macOS, Linux)
     * @return информация о версии
     */
    VersionInfoResponseDto getVersionInfo(String currentVersion, String platform);
    
    /**
     * Получает ссылку на скачивание обновления
     * 
     * @param currentVersion текущая версия приложения пользователя
     * @param targetVersion версия для обновления (опционально)
     * @param platform платформа (Windows, macOS, Linux)
     * @param arch архитектура (x64, arm64)
     * @return ссылка на скачивание
     */
    AppUpdateResponseDto getUpdateLink(String currentVersion, String targetVersion, String platform, String arch);
    
    /**
     * Получает список изменений приложения
     * 
     * @param from начальная версия (опционально)
     * @param to конечная версия (опционально)
     * @param limit максимальное количество версий
     * @return список изменений
     */
    List<ChangelogEntryDto> getChangelog(String from, String to, int limit);
} 