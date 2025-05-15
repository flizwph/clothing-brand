package com.brand.backend.application.desktop.service.impl;

import com.brand.backend.application.desktop.service.DesktopUpdateService;
import com.brand.backend.presentation.dto.response.desktop.AppUpdateResponseDto;
import com.brand.backend.presentation.dto.response.desktop.ChangelogEntryDto;
import com.brand.backend.presentation.dto.response.desktop.VersionInfoResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Реализация сервиса для управления обновлениями десктопного приложения
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DesktopUpdateServiceImpl implements DesktopUpdateService {

    /**
     * Получает информацию о последней версии приложения
     * 
     * @param currentVersion текущая версия приложения пользователя
     * @param platform платформа (Windows, macOS, Linux)
     * @return информация о версии
     */
    @Override
    public VersionInfoResponseDto getVersionInfo(String currentVersion, String platform) {
        log.info("Получение информации о версии для платформы: {}, текущая версия: {}", 
                platform, currentVersion);
        
        // TODO: Реализовать получение информации о версии из базы данных
        
        // Опеределяем, доступно ли обновление
        boolean updateAvailable = compareVersions(currentVersion, "1.2.0") < 0;
        
        // Определяем, обязательное ли обновление
        boolean mandatoryUpdate = compareVersions(currentVersion, "1.0.0") <= 0;
        
        // Создаем демонстрационный ответ
        return VersionInfoResponseDto.builder()
                .latestVersion("1.2.0")
                .releaseDate(LocalDateTime.now().minusDays(7))
                .mandatoryUpdate(mandatoryUpdate)
                .minVersionForUpdate("1.0.0")
                .updateAvailable(updateAvailable)
                .changelog(Arrays.asList(
                        "Добавлена поддержка синхронизации с облаком",
                        "Улучшена производительность приложения",
                        "Исправлены ошибки в работе с файлами"
                ))
                .downloadUrl("https://brand.com/downloads/app/1.2.0/" + platform.toLowerCase() + "/brand-app-installer.exe")
                .downloadSize(25600000L)
                .downloadHash("a1b2c3d4e5f6g7h8i9j0klmnopqrstuvwxyz12345678")
                .additionalInfo(new HashMap<>())
                .build();
    }

    /**
     * Получает ссылку на скачивание обновления
     * 
     * @param currentVersion текущая версия приложения пользователя
     * @param targetVersion версия для обновления (опционально)
     * @param platform платформа (Windows, macOS, Linux)
     * @param arch архитектура (x64, arm64)
     * @return ссылка на скачивание
     */
    @Override
    public AppUpdateResponseDto getUpdateLink(String currentVersion, String targetVersion, String platform, String arch) {
        log.info("Получение ссылки на обновление для платформы: {}, архитектура: {}, " +
                "текущая версия: {}, целевая версия: {}", 
                platform, arch, currentVersion, targetVersion);
        
        // Если целевая версия не указана, используем последнюю
        String version = targetVersion != null ? targetVersion : "1.2.0";
        
        // Определяем тип обновления (полное или патч)
        String updateType = compareVersions(currentVersion, "1.0.0") <= 0 ? "FULL" : "PATCH";
        
        // TODO: Реализовать получение ссылки на обновление из базы данных
        
        // Формируем URL для скачивания
        String downloadUrl = String.format(
                "https://brand.com/downloads/app/%s/%s/%s/brand-app-%s.%s",
                version,
                platform.toLowerCase(),
                arch.toLowerCase(),
                version,
                platform.equalsIgnoreCase("windows") ? "exe" : 
                platform.equalsIgnoreCase("macos") ? "dmg" : "tar.gz"
        );
        
        // Создаем демонстрационный ответ
        return AppUpdateResponseDto.builder()
                .version(version)
                .downloadUrl(downloadUrl)
                .downloadSize(25600000L)
                .downloadHash("a1b2c3d4e5f6g7h8i9j0klmnopqrstuvwxyz12345678")
                .updateType(updateType)
                .requiresRestart(true)
                .releaseDate(LocalDateTime.now().minusDays(7))
                .changelog(generateChangelogEntry(version))
                .installationInstructions("Запустите скачанный файл и следуйте инструкциям установщика.")
                .systemRequirements(Map.of(
                        "os", platform + " 10+",
                        "cpu", "2 ГГц двухъядерный процессор или лучше",
                        "ram", "4 ГБ RAM",
                        "disk", "500 МБ свободного места на диске"
                ))
                .build();
    }

    /**
     * Получает список изменений приложения
     * 
     * @param from начальная версия (опционально)
     * @param to конечная версия (опционально)
     * @param limit максимальное количество версий
     * @return список изменений
     */
    @Override
    public List<ChangelogEntryDto> getChangelog(String from, String to, int limit) {
        log.info("Получение списка изменений от версии: {} до версии: {}, лимит: {}", 
                from, to, limit);
        
        // TODO: Реализовать получение списка изменений из базы данных
        
        // Создаем демонстрационный ответ
        List<ChangelogEntryDto> changelog = new ArrayList<>();
        
        // Добавляем версию 1.2.0
        changelog.add(generateChangelogEntry("1.2.0"));
        
        // Добавляем версию 1.1.0
        ChangelogEntryDto entry11 = ChangelogEntryDto.builder()
                .version("1.1.0")
                .releaseDate(LocalDateTime.now().minusDays(30))
                .added(Arrays.asList(
                        "Добавлена возможность экспорта данных в CSV",
                        "Добавлены новые шаблоны дизайна"
                ))
                .improved(Arrays.asList(
                        "Улучшен интерфейс редактора",
                        "Оптимизирована работа с большими файлами"
                ))
                .fixed(Arrays.asList(
                        "Исправлена ошибка при сохранении настроек",
                        "Исправлены проблемы с отображением шрифтов"
                ))
                .removed(Collections.singletonList(
                        "Удалена устаревшая функция импорта из версии 0.9"
                ))
                .build();
        changelog.add(entry11);
        
        // Добавляем версию 1.0.0
        ChangelogEntryDto entry10 = ChangelogEntryDto.builder()
                .version("1.0.0")
                .releaseDate(LocalDateTime.now().minusDays(60))
                .added(Arrays.asList(
                        "Первая стабильная версия приложения",
                        "Полная поддержка всех основных функций"
                ))
                .improved(Collections.emptyList())
                .fixed(Collections.emptyList())
                .removed(Collections.emptyList())
                .additionalNotes("Это первый официальный релиз нашего приложения!")
                .build();
        changelog.add(entry10);
        
        // Ограничиваем количество версий в ответе
        return changelog.subList(0, Math.min(limit, changelog.size()));
    }
    
    /**
     * Сравнивает две версии в формате "x.y.z"
     * 
     * @param version1 первая версия
     * @param version2 вторая версия
     * @return отрицательное число, если version1 < version2,
     *         положительное число, если version1 > version2,
     *         0, если version1 == version2
     */
    private int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int length = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (v1 < v2) {
                return -1;
            } else if (v1 > v2) {
                return 1;
            }
        }
        
        return 0;
    }
    
    /**
     * Генерирует демонстрационные данные о списке изменений для версии
     * 
     * @param version версия приложения
     * @return информация о списке изменений
     */
    private ChangelogEntryDto generateChangelogEntry(String version) {
        if ("1.2.0".equals(version)) {
            return ChangelogEntryDto.builder()
                    .version(version)
                    .releaseDate(LocalDateTime.now().minusDays(7))
                    .added(Arrays.asList(
                            "Добавлена поддержка синхронизации с облаком",
                            "Добавлена возможность работы в офлайн-режиме",
                            "Добавлены новые инструменты редактирования"
                    ))
                    .improved(Arrays.asList(
                            "Улучшена производительность приложения",
                            "Оптимизирована работа с памятью",
                            "Улучшен интерфейс пользователя"
                    ))
                    .fixed(Arrays.asList(
                            "Исправлены ошибки в работе с файлами",
                            "Исправлена проблема с автосохранением",
                            "Устранены утечки памяти при длительной работе"
                    ))
                    .knownIssues(Collections.singletonList(
                            "При редактировании больших файлов может наблюдаться замедление"
                    ))
                    .technical(Map.of(
                            "dependencies", Arrays.asList(
                                    "Обновлены зависимости фреймворка до актуальной версии",
                                    "Добавлена новая библиотека для работы с облаком"
                            ),
                            "database", Collections.singletonList(
                                    "Обновлена схема локальной базы данных"
                            )
                    ))
                    .build();
        } else {
            return ChangelogEntryDto.builder()
                    .version(version)
                    .releaseDate(LocalDateTime.now().minusDays(1))
                    .added(Collections.singletonList("Информация недоступна"))
                    .build();
        }
    }
} 