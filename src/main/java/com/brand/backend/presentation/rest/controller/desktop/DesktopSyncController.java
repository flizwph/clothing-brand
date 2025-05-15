package com.brand.backend.presentation.rest.controller.desktop;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.SettingsSyncRequest;
import com.brand.backend.presentation.dto.request.desktop.DataSyncRequest;
import com.brand.backend.presentation.dto.request.desktop.OfflineChangesRequest;
import com.brand.backend.presentation.dto.response.ApiResponse;
import com.brand.backend.presentation.dto.response.desktop.DataSyncResponseDto;
import com.brand.backend.presentation.dto.response.desktop.OfflineChangesResponseDto;
import com.brand.backend.presentation.dto.response.desktop.SettingsSyncResponseDto;
import com.brand.backend.presentation.dto.response.desktop.SyncStatusResponseDto;
import com.brand.backend.application.desktop.service.DesktopSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

/**
 * Контроллер для синхронизации данных десктопного приложения
 */
@RestController
@RequestMapping("/api/desktop/sync")
@RequiredArgsConstructor
@Slf4j
public class DesktopSyncController {

    private final DesktopSyncService syncService;

    /**
     * Получение настроек пользователя для синхронизации
     * 
     * @param user аутентифицированный пользователь
     * @return настройки пользователя
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<SettingsSyncResponseDto>> getUserSettings(
            @AuthenticationPrincipal User user) {
        
        log.info("Получение настроек для пользователя: {}", user.getUsername());
        
        SettingsSyncResponseDto settings = syncService.getUserSettings(user);
        
        return ResponseEntity.ok(new ApiResponse<>(settings));
    }

    /**
     * Обновление настроек пользователя
     * 
     * @param request данные для обновления настроек
     * @param user аутентифицированный пользователь
     * @return результат обновления настроек
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<SettingsSyncResponseDto>> updateUserSettings(
            @Valid @RequestBody SettingsSyncRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Обновление настроек для пользователя: {}", user.getUsername());
        
        SettingsSyncResponseDto settings = syncService.updateUserSettings(request, user);
        
        return ResponseEntity.ok(new ApiResponse<>(settings));
    }

    /**
     * Получение пользовательских данных для синхронизации
     * 
     * @param lastSync временная метка последней синхронизации (опционально)
     * @param dataType тип данных для синхронизации (опционально)
     * @param user аутентифицированный пользователь
     * @return данные пользователя
     */
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<DataSyncResponseDto>> getUserData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastSync,
            @RequestParam(required = false) String dataType,
            @AuthenticationPrincipal User user) {
        
        log.info("Получение данных типа [{}] для пользователя: {}, lastSync: {}", 
                dataType, user.getUsername(), lastSync);
        
        DataSyncResponseDto data = syncService.getUserData(user, lastSync, dataType);
        
        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    /**
     * Синхронизация пользовательских данных
     * 
     * @param request данные для синхронизации
     * @param user аутентифицированный пользователь
     * @return результат синхронизации
     */
    @PostMapping("/data")
    public ResponseEntity<ApiResponse<DataSyncResponseDto>> syncUserData(
            @Valid @RequestBody DataSyncRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Синхронизация данных типа [{}] для пользователя: {}", 
                request.getDataType(), user.getUsername());
        
        DataSyncResponseDto data = syncService.syncUserData(request, user);
        
        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    /**
     * Получение статуса синхронизации
     * 
     * @param user аутентифицированный пользователь
     * @return статус синхронизации
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SyncStatusResponseDto>> getSyncStatus(
            @AuthenticationPrincipal User user) {
        
        log.info("Получение статуса синхронизации для пользователя: {}", user.getUsername());
        
        SyncStatusResponseDto status = syncService.getSyncStatus(user);
        
        return ResponseEntity.ok(new ApiResponse<>(status));
    }

    /**
     * Синхронизация офлайн-изменений
     * 
     * @param request данные для синхронизации офлайн-изменений
     * @param user аутентифицированный пользователь
     * @return результат синхронизации
     */
    @PostMapping("/offline-changes")
    public ResponseEntity<ApiResponse<OfflineChangesResponseDto>> syncOfflineChanges(
            @Valid @RequestBody OfflineChangesRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Синхронизация офлайн-изменений для пользователя: {}", user.getUsername());
        
        OfflineChangesResponseDto result = syncService.syncOfflineChanges(request, user);
        
        return ResponseEntity.ok(new ApiResponse<>(result));
    }
} 