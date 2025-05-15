package com.brand.backend.application.desktop.service.impl;

import com.brand.backend.application.desktop.service.DesktopLicenseService;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.LicenseActivationRequest;
import com.brand.backend.presentation.dto.request.desktop.LicenseDeactivationRequest;
import com.brand.backend.presentation.dto.request.desktop.LicenseValidationRequest;
import com.brand.backend.presentation.dto.request.desktop.OfflineLicenseCheckRequest;
import com.brand.backend.presentation.dto.response.desktop.InstallationResponseDto;
import com.brand.backend.presentation.dto.response.desktop.LicenseFeaturesDto;
import com.brand.backend.presentation.dto.response.desktop.LicenseStatusResponseDto;
import com.brand.backend.presentation.dto.response.desktop.OfflineLicenseResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Реализация сервиса для управления лицензиями десктопного приложения
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DesktopLicenseServiceImpl implements DesktopLicenseService {

    /**
     * Проверяет действительность лицензии
     * 
     * @param request данные для проверки
     * @param user пользователь
     * @return статус лицензии
     */
    @Override
    @Transactional(readOnly = true)
    public LicenseStatusResponseDto validateLicense(LicenseValidationRequest request, User user) {
        log.info("Проверка лицензии: {}, пользователь: {}", request.getLicenseKey(), user.getUsername());
        
        // TODO: Реализовать проверку лицензии в базе данных
        
        // Создаем демонстрационный ответ
        return LicenseStatusResponseDto.builder()
                .valid(true)
                .licenseType("PROFESSIONAL")
                .activationDate(LocalDateTime.now().minusDays(30))
                .expirationDate(LocalDateTime.now().plusYears(1))
                .activeInstallations(1)
                .maxInstallations(3)
                .licensedTo(user.getEmail())
                .features(LicenseFeaturesDto.builder()
                        .premiumFeatures(true)
                        .advancedEditing(true)
                        .dataImport(true)
                        .dataExport(true)
                        .offlineMode(true)
                        .maxOfflineDays(30)
                        .cloudStorage(true)
                        .cloudStorageSize(10240)
                        .availablePlugins(List.of("advanced-tools", "templates", "statistics"))
                        .additionalFeatures(new HashMap<>())
                        .build())
                .refreshToken(UUID.randomUUID().toString())
                .nextValidationDate(LocalDateTime.now().plusDays(7))
                .build();
    }

    /**
     * Регистрирует новую установку приложения для лицензии
     * 
     * @param request данные для регистрации
     * @param user пользователь
     * @return данные об установке
     */
    @Override
    @Transactional
    public InstallationResponseDto registerInstallation(LicenseActivationRequest request, User user) {
        log.info("Регистрация установки для лицензии: {}, пользователь: {}", 
                request.getLicenseKey(), user.getUsername());
        
        // TODO: Реализовать сохранение данных об установке в базе данных
        
        // Создаем демонстрационный ответ
        return InstallationResponseDto.builder()
                .id(1L)
                .licenseKey(request.getLicenseKey())
                .deviceName(request.getDeviceName())
                .deviceId(request.getDeviceId())
                .osInfo(request.getOsInfo())
                .activationDate(LocalDateTime.now())
                .lastActivityDate(LocalDateTime.now())
                .active(true)
                .activationToken(UUID.randomUUID().toString())
                .licenseInfo(validateLicense(
                        LicenseValidationRequest.builder()
                                .licenseKey(request.getLicenseKey())
                                .deviceId(request.getDeviceId())
                                .securityKey(request.getSecurityKey())
                                .build(), 
                        user))
                .build();
    }

    /**
     * Деактивирует существующую установку
     * 
     * @param request данные для деактивации
     * @param user пользователь
     * @return данные об установке после деактивации
     */
    @Override
    @Transactional
    public InstallationResponseDto deactivateInstallation(LicenseDeactivationRequest request, User user) {
        log.info("Деактивация установки для лицензии: {}, пользователь: {}", 
                request.getLicenseKey(), user.getUsername());
        
        // TODO: Реализовать деактивацию установки в базе данных
        
        // Создаем демонстрационный ответ
        return InstallationResponseDto.builder()
                .id(1L)
                .licenseKey(request.getLicenseKey())
                .deviceName("Деактивированное устройство")
                .deviceId(request.getDeviceId())
                .osInfo("Unknown")
                .activationDate(LocalDateTime.now().minusDays(30))
                .lastActivityDate(LocalDateTime.now())
                .active(false)
                .activationToken(null)
                .build();
    }

    /**
     * Генерирует токен для работы в офлайн-режиме
     * 
     * @param request данные для генерации офлайн-токена
     * @return данные офлайн-токена
     */
    @Override
    @Transactional
    public OfflineLicenseResponseDto generateOfflineToken(OfflineLicenseCheckRequest request) {
        log.info("Генерация офлайн-токена для лицензии: {}", request.getLicenseKey());
        
        // Определяем количество дней офлайн-работы
        int validDays = request.getRequestedDays() != null 
                ? Math.min(request.getRequestedDays(), 30) // Максимум 30 дней
                : 7; // По умолчанию 7 дней
        
        // TODO: Реализовать генерацию и сохранение офлайн-токена в базе данных
        
        // Создаем демонстрационный ответ
        return OfflineLicenseResponseDto.builder()
                .offlineToken(UUID.randomUUID().toString())
                .expirationDate(LocalDateTime.now().plusDays(validDays))
                .validDays(validDays)
                .deviceId(request.getDeviceId())
                .licenseKey(request.getLicenseKey())
                .features(LicenseFeaturesDto.builder()
                        .premiumFeatures(true)
                        .advancedEditing(true)
                        .dataImport(true)
                        .dataExport(true)
                        .offlineMode(true)
                        .maxOfflineDays(validDays)
                        .cloudStorage(false) // Облачное хранилище недоступно в офлайне
                        .availablePlugins(new ArrayList<>()) // Плагины недоступны в офлайне
                        .additionalFeatures(new HashMap<>())
                        .build())
                .build();
    }
} 