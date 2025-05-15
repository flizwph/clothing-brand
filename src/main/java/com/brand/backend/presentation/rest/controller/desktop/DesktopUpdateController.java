package com.brand.backend.presentation.rest.controller.desktop;

import com.brand.backend.presentation.dto.response.ApiResponse;
import com.brand.backend.presentation.dto.response.desktop.AppUpdateResponseDto;
import com.brand.backend.presentation.dto.response.desktop.ChangelogEntryDto;
import com.brand.backend.presentation.dto.response.desktop.VersionInfoResponseDto;
import com.brand.backend.application.desktop.service.DesktopUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для управления обновлениями десктопного приложения
 */
@RestController
@RequestMapping("/api/desktop")
@RequiredArgsConstructor
@Slf4j
public class DesktopUpdateController {

    private final DesktopUpdateService updateService;

    /**
     * Получение информации о последней версии приложения
     * 
     * @param currentVersion текущая версия приложения пользователя
     * @param platform платформа (Windows, macOS, Linux)
     * @return информация о версии
     */
    @GetMapping("/version")
    public ResponseEntity<ApiResponse<VersionInfoResponseDto>> getVersionInfo(
            @RequestParam String currentVersion,
            @RequestParam String platform) {
        
        log.info("Получение информации о версии для платформы: {}, текущая версия: {}", 
                platform, currentVersion);
        
        VersionInfoResponseDto versionInfo = updateService.getVersionInfo(currentVersion, platform);
        
        return ResponseEntity.ok(new ApiResponse<>(versionInfo));
    }

    /**
     * Получение ссылки на скачивание обновления
     * 
     * @param currentVersion текущая версия приложения пользователя
     * @param targetVersion версия для обновления (опционально)
     * @param platform платформа (Windows, macOS, Linux)
     * @param arch архитектура (x64, arm64)
     * @return ссылка на скачивание
     */
    @GetMapping("/update")
    public ResponseEntity<ApiResponse<AppUpdateResponseDto>> getUpdateLink(
            @RequestParam String currentVersion,
            @RequestParam(required = false) String targetVersion,
            @RequestParam String platform,
            @RequestParam String arch) {
        
        log.info("Получение ссылки на обновление для платформы: {}, архитектура: {}, " +
                "текущая версия: {}, целевая версия: {}", 
                platform, arch, currentVersion, targetVersion);
        
        AppUpdateResponseDto updateLink = updateService.getUpdateLink(
                currentVersion, targetVersion, platform, arch);
        
        return ResponseEntity.ok(new ApiResponse<>(updateLink));
    }

    /**
     * Получение списка изменений приложения
     * 
     * @param from начальная версия (опционально)
     * @param to конечная версия (опционально)
     * @param limit максимальное количество версий
     * @return список изменений
     */
    @GetMapping("/changelog")
    public ResponseEntity<ApiResponse<List<ChangelogEntryDto>>> getChangelog(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Получение списка изменений от версии: {} до версии: {}, лимит: {}", 
                from, to, limit);
        
        List<ChangelogEntryDto> changelog = updateService.getChangelog(from, to, limit);
        
        return ResponseEntity.ok(new ApiResponse<>(changelog));
    }
} 