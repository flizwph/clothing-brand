package com.brand.backend.application.desktop.service.impl;

import com.brand.backend.application.desktop.service.DesktopProductService;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.ProductActivationRequest;
import com.brand.backend.presentation.dto.response.desktop.DesktopProductActivationResponseDto;
import com.brand.backend.presentation.dto.response.desktop.DesktopProductDownloadResponseDto;
import com.brand.backend.presentation.dto.response.desktop.DesktopProductResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Реализация сервиса для работы с цифровыми продуктами в десктопном приложении
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DesktopProductServiceImpl implements DesktopProductService {

    /**
     * Получает список цифровых продуктов для десктопного приложения
     * 
     * @param user пользователь
     * @param includeOwned включать уже приобретенные продукты
     * @param type тип продукта
     * @param pageable параметры пагинации
     * @return список продуктов
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DesktopProductResponseDto> getDesktopProducts(
            User user, 
            boolean includeOwned, 
            String type, 
            Pageable pageable) {
        
        log.info("Получение списка продуктов для пользователя: {}, тип: {}, includeOwned: {}", 
                user.getUsername(), type, includeOwned);
        
        // TODO: Реализовать получение продуктов из базы данных
        
        // Создаем демонстрационные данные
        List<DesktopProductResponseDto> products = new ArrayList<>();
        
        // Примеры продуктов
        products.add(createSampleProduct(1L, "Дизайнерский шаблон - Минимализм", "template", true));
        products.add(createSampleProduct(2L, "Набор кистей для фотошопа", "brushes", false));
        products.add(createSampleProduct(3L, "Учебник по дизайну одежды", "ebook", false));
        
        // Фильтрация по типу (если указан)
        if (type != null && !type.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getType().equals(type))
                    .toList();
        }
        
        // Фильтрация по владению
        if (!includeOwned) {
            products = products.stream()
                    .filter(p -> !p.isOwned())
                    .toList();
        }
        
        // Пагинация
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), products.size());
        
        // Если start больше размера списка, возвращаем пустую страницу
        if (start >= products.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, products.size());
        }
        
        return new PageImpl<>(
                products.subList(start, end),
                pageable,
                products.size()
        );
    }
    
    /**
     * Получает ссылку на скачивание цифрового продукта
     * 
     * @param productId идентификатор продукта
     * @param user пользователь
     * @return ссылка на скачивание
     */
    @Override
    @Transactional(readOnly = true)
    public DesktopProductDownloadResponseDto getProductDownloadLink(Long productId, User user) {
        log.info("Получение ссылки на скачивание продукта с ID: {} для пользователя: {}", 
                productId, user.getUsername());
        
        // TODO: Реализовать проверку доступа и генерацию ссылки на скачивание
        
        // Создаем демонстрационный ответ
        return DesktopProductDownloadResponseDto.builder()
                .productId(productId)
                .productName("Дизайнерский шаблон - Минимализм")
                .downloadUrl("https://brand.com/downloads/product/" + productId + "/download?token=sample-token")
                .fallbackDownloadUrl("https://brand-fallback.com/downloads/product/" + productId + "/download?token=sample-token")
                .accessToken(UUID.randomUUID().toString())
                .fileSize(15728640L) // 15 МБ
                .fileHash("a1b2c3d4e5f6g7h8i9j0klmnopqrstuvwxyz")
                .hashAlgorithm("SHA-256")
                .expirationTime(LocalDateTime.now().plusHours(24))
                .installationInstructions("Распакуйте архив и импортируйте шаблон в приложение.")
                .version("1.0")
                .fileType("zip")
                .build();
    }
    
    /**
     * Активирует цифровой продукт по коду активации
     * 
     * @param activationCode код активации
     * @param request данные для активации
     * @param user пользователь
     * @return результат активации
     */
    @Override
    @Transactional
    public DesktopProductActivationResponseDto activateProduct(
            String activationCode,
            ProductActivationRequest request,
            User user) {
        
        log.info("Активация продукта с кодом: {} для пользователя: {}", activationCode, user.getUsername());
        
        // TODO: Реализовать проверку кода активации и активацию продукта
        
        // Создаем демонстрационный ответ
        return DesktopProductActivationResponseDto.builder()
                .productId(1L)
                .productName("Дизайнерский шаблон - Минимализм")
                .activationCode(activationCode)
                .status("SUCCESS")
                .message("Продукт успешно активирован.")
                .deviceId(request.getDeviceId())
                .activationTime(LocalDateTime.now())
                .expirationTime(null) // Бессрочная лицензия
                .activationKey(UUID.randomUUID().toString())
                .productDetails(Map.of(
                        "version", "1.0",
                        "type", "template",
                        "description", "Минималистичный шаблон для дизайна одежды"
                ))
                .unlockedFeatures(Map.of(
                        "export", true,
                        "edit", true,
                        "cloud_sync", true
                ))
                .build();
    }
    
    /**
     * Создает пример продукта для демонстрации
     * 
     * @param id идентификатор продукта
     * @param name название продукта
     * @param type тип продукта
     * @param owned флаг владения
     * @return пример продукта
     */
    private DesktopProductResponseDto createSampleProduct(Long id, String name, String type, boolean owned) {
        return DesktopProductResponseDto.builder()
                .id(id)
                .name(name)
                .description("Детальное описание продукта " + name)
                .type(type)
                .version("1.0")
                .price(new BigDecimal("19.99"))
                .discountPercent(type.equals("template") ? 15 : 0)
                .previewUrl("https://brand.com/preview/product/" + id)
                .coverImageUrl("https://brand.com/images/product/" + id + "/cover.jpg")
                .galleryImageUrls(Arrays.asList(
                        "https://brand.com/images/product/" + id + "/gallery1.jpg",
                        "https://brand.com/images/product/" + id + "/gallery2.jpg"
                ))
                .fileSize(type.equals("ebook") ? 5242880L : 15728640L) // 5 МБ или 15 МБ
                .owned(owned)
                .purchaseDate(owned ? LocalDateTime.now().minusDays(30) : null)
                .tags(Arrays.asList("design", type, "digital"))
                .systemRequirements(Map.of(
                        "os", "Windows 10, macOS 10.14+",
                        "ram", "4 ГБ"
                ))
                .averageRating(4.5)
                .ratingCount(42)
                .build();
    }
} 