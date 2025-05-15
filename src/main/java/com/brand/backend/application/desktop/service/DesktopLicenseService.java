package com.brand.backend.application.desktop.service;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.LicenseActivationRequest;
import com.brand.backend.presentation.dto.request.desktop.LicenseDeactivationRequest;
import com.brand.backend.presentation.dto.request.desktop.LicenseValidationRequest;
import com.brand.backend.presentation.dto.request.desktop.OfflineLicenseCheckRequest;
import com.brand.backend.presentation.dto.response.desktop.InstallationResponseDto;
import com.brand.backend.presentation.dto.response.desktop.LicenseStatusResponseDto;
import com.brand.backend.presentation.dto.response.desktop.OfflineLicenseResponseDto;

/**
 * Сервис для управления лицензиями десктопного приложения
 */
public interface DesktopLicenseService {

    /**
     * Проверяет действительность лицензии
     * 
     * @param request данные для проверки
     * @param user пользователь
     * @return статус лицензии
     */
    LicenseStatusResponseDto validateLicense(LicenseValidationRequest request, User user);
    
    /**
     * Регистрирует новую установку приложения для лицензии
     * 
     * @param request данные для регистрации
     * @param user пользователь
     * @return данные об установке
     */
    InstallationResponseDto registerInstallation(LicenseActivationRequest request, User user);
    
    /**
     * Деактивирует существующую установку
     * 
     * @param request данные для деактивации
     * @param user пользователь
     * @return данные об установке после деактивации
     */
    InstallationResponseDto deactivateInstallation(LicenseDeactivationRequest request, User user);
    
    /**
     * Генерирует токен для работы в офлайн-режиме
     * 
     * @param request данные для генерации офлайн-токена
     * @return данные офлайн-токена
     */
    OfflineLicenseResponseDto generateOfflineToken(OfflineLicenseCheckRequest request);
} 