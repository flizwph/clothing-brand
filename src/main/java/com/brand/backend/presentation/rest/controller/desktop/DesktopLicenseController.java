package com.brand.backend.presentation.rest.controller.desktop;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.LicenseActivationRequest;
import com.brand.backend.presentation.dto.request.desktop.LicenseDeactivationRequest;
import com.brand.backend.presentation.dto.request.desktop.LicenseValidationRequest;
import com.brand.backend.presentation.dto.request.desktop.OfflineLicenseCheckRequest;
import com.brand.backend.presentation.dto.response.ApiResponse;
import com.brand.backend.presentation.dto.response.desktop.InstallationResponseDto;
import com.brand.backend.presentation.dto.response.desktop.LicenseStatusResponseDto;
import com.brand.backend.presentation.dto.response.desktop.OfflineLicenseResponseDto;
import com.brand.backend.application.desktop.service.DesktopLicenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

/**
 * Контроллер для управления лицензиями десктопного приложения
 */
@RestController
@RequestMapping("/api/desktop")
@RequiredArgsConstructor
@Slf4j
public class DesktopLicenseController {

    private final DesktopLicenseService licenseService;

    /**
     * Проверка действительности лицензии
     * 
     * @param request данные для проверки лицензии
     * @param user аутентифицированный пользователь
     * @return статус лицензии
     */
    @PostMapping("/validate-license")
    public ResponseEntity<ApiResponse<LicenseStatusResponseDto>> validateLicense(
            @Valid @RequestBody LicenseValidationRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Проверка лицензии пользователя: {}", user.getUsername());
        
        LicenseStatusResponseDto licenseStatus = licenseService.validateLicense(request, user);
        
        return ResponseEntity.ok(new ApiResponse<>(licenseStatus));
    }

    /**
     * Регистрация новой установки десктопного приложения
     * 
     * @param request данные для регистрации установки
     * @param user аутентифицированный пользователь
     * @return информация о зарегистрированной установке
     */
    @PostMapping("/register-installation")
    public ResponseEntity<ApiResponse<InstallationResponseDto>> registerInstallation(
            @Valid @RequestBody LicenseActivationRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Регистрация новой установки для пользователя: {}", user.getUsername());
        
        InstallationResponseDto installation = licenseService.registerInstallation(request, user);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(installation));
    }

    /**
     * Деактивация существующей установки
     * 
     * @param request данные для деактивации установки
     * @param user аутентифицированный пользователь
     * @return результат деактивации
     */
    @PostMapping("/deactivate-installation")
    public ResponseEntity<ApiResponse<InstallationResponseDto>> deactivateInstallation(
            @Valid @RequestBody LicenseDeactivationRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Деактивация установки для пользователя: {}", user.getUsername());
        
        InstallationResponseDto installation = licenseService.deactivateInstallation(request, user);
        
        return ResponseEntity.ok(new ApiResponse<>(installation));
    }
    
    /**
     * Проверка лицензии для работы в офлайн-режиме
     * 
     * @param request данные для проверки лицензии в офлайн-режиме
     * @return офлайн-токен
     */
    @PostMapping("/license/offline-check")
    public ResponseEntity<ApiResponse<OfflineLicenseResponseDto>> offlineLicenseCheck(
            @Valid @RequestBody OfflineLicenseCheckRequest request) {
        
        log.info("Запрос на получение офлайн-токена для лицензии: {}", request.getLicenseKey());
        
        OfflineLicenseResponseDto offlineToken = licenseService.generateOfflineToken(request);
        
        return ResponseEntity.ok(new ApiResponse<>(offlineToken));
    }
} 