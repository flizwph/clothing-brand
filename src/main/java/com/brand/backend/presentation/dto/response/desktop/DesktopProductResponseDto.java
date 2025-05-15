package com.brand.backend.presentation.dto.response.desktop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для ответа с информацией о цифровом продукте
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesktopProductResponseDto {

    /**
     * Идентификатор продукта
     */
    private Long id;
    
    /**
     * Название продукта
     */
    private String name;
    
    /**
     * Описание продукта
     */
    private String description;
    
    /**
     * Тип продукта
     */
    private String type;
    
    /**
     * Версия продукта
     */
    private String version;
    
    /**
     * Цена продукта
     */
    private BigDecimal price;
    
    /**
     * Скидка (в процентах)
     */
    private Integer discountPercent;
    
    /**
     * URL предварительного просмотра
     */
    private String previewUrl;
    
    /**
     * URL изображения обложки
     */
    private String coverImageUrl;
    
    /**
     * Список URL изображений галереи
     */
    private List<String> galleryImageUrls;
    
    /**
     * Размер файла (в байтах)
     */
    private Long fileSize;
    
    /**
     * Флаг доступности для пользователя
     */
    private boolean owned;
    
    /**
     * Дата покупки (если куплен)
     */
    private LocalDateTime purchaseDate;
    
    /**
     * Дата истечения срока действия (если временный)
     */
    private LocalDateTime expirationDate;
    
    /**
     * Список тегов продукта
     */
    private List<String> tags;
    
    /**
     * Требования к системе
     */
    private Map<String, String> systemRequirements;
    
    /**
     * Средний рейтинг (от 1 до 5)
     */
    private Double averageRating;
    
    /**
     * Количество оценок
     */
    private Integer ratingCount;
} 