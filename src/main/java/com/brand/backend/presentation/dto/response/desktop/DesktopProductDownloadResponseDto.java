package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для ответа с информацией о ссылке на скачивание цифрового продукта
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesktopProductDownloadResponseDto {

    /**
     * Идентификатор продукта
     */
    private Long productId;
    
    /**
     * Название продукта
     */
    private String productName;
    
    /**
     * URL для скачивания
     */
    private String downloadUrl;
    
    /**
     * URL для резервного скачивания
     */
    private String fallbackDownloadUrl;
    
    /**
     * Токен безопасности для доступа
     */
    private String accessToken;
    
    /**
     * Размер файла (в байтах)
     */
    private Long fileSize;
    
    /**
     * Хэш-сумма файла (для проверки целостности)
     */
    private String fileHash;
    
    /**
     * Алгоритм хэш-суммы
     */
    private String hashAlgorithm;
    
    /**
     * Время окончания действия ссылки
     */
    private LocalDateTime expirationTime;
    
    /**
     * Инструкции по установке
     */
    private String installationInstructions;
    
    /**
     * Версия продукта
     */
    private String version;
    
    /**
     * Тип файла
     */
    private String fileType;
} 