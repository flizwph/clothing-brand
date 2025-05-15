package com.brand.backend.application.desktop.service;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.ProductActivationRequest;
import com.brand.backend.presentation.dto.response.desktop.DesktopProductActivationResponseDto;
import com.brand.backend.presentation.dto.response.desktop.DesktopProductDownloadResponseDto;
import com.brand.backend.presentation.dto.response.desktop.DesktopProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Сервис для работы с цифровыми продуктами в десктопном приложении
 */
public interface DesktopProductService {

    /**
     * Получает список цифровых продуктов для десктопного приложения
     * 
     * @param user пользователь
     * @param includeOwned включать уже приобретенные продукты
     * @param type тип продукта
     * @param pageable параметры пагинации
     * @return список продуктов
     */
    Page<DesktopProductResponseDto> getDesktopProducts(
            User user, 
            boolean includeOwned, 
            String type, 
            Pageable pageable);
    
    /**
     * Получает ссылку на скачивание цифрового продукта
     * 
     * @param productId идентификатор продукта
     * @param user пользователь
     * @return ссылка на скачивание
     */
    DesktopProductDownloadResponseDto getProductDownloadLink(Long productId, User user);
    
    /**
     * Активирует цифровой продукт по коду активации
     * 
     * @param activationCode код активации
     * @param request данные для активации
     * @param user пользователь
     * @return результат активации
     */
    DesktopProductActivationResponseDto activateProduct(
            String activationCode,
            ProductActivationRequest request,
            User user);
} 