package com.brand.backend.presentation.rest.controller.desktop;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.ProductActivationRequest;
import com.brand.backend.presentation.dto.response.ApiResponse;
import com.brand.backend.presentation.dto.response.desktop.DesktopProductActivationResponseDto;
import com.brand.backend.presentation.dto.response.desktop.DesktopProductDownloadResponseDto;
import com.brand.backend.presentation.dto.response.desktop.DesktopProductResponseDto;
import com.brand.backend.application.desktop.service.DesktopProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Контроллер для работы с цифровыми продуктами в десктопном приложении
 */
@RestController
@RequestMapping("/api/desktop/products")
@RequiredArgsConstructor
@Slf4j
public class DesktopProductController {

    private final DesktopProductService desktopProductService;

    /**
     * Получение списка цифровых продуктов для десктопного приложения
     * 
     * @param includeOwned включать уже приобретенные продукты
     * @param type тип продукта
     * @param pageable параметры пагинации
     * @param user аутентифицированный пользователь
     * @return список продуктов
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DesktopProductResponseDto>>> getDesktopProducts(
            @RequestParam(defaultValue = "true") boolean includeOwned,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {
        
        log.info("Получение списка продуктов для пользователя: {}, тип: {}, includeOwned: {}", 
                user.getUsername(), type, includeOwned);
        
        Page<DesktopProductResponseDto> products = desktopProductService.getDesktopProducts(
                user, includeOwned, type, pageable);
        
        return ResponseEntity.ok(new ApiResponse<>(products));
    }

    /**
     * Получение ссылки на скачивание цифрового продукта
     * 
     * @param id идентификатор продукта
     * @param user аутентифицированный пользователь
     * @return ссылка на скачивание
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<ApiResponse<DesktopProductDownloadResponseDto>> getProductDownloadLink(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        
        log.info("Получение ссылки на скачивание продукта с ID: {} для пользователя: {}", 
                id, user.getUsername());
        
        DesktopProductDownloadResponseDto downloadLink = desktopProductService.getProductDownloadLink(id, user);
        
        return ResponseEntity.ok(new ApiResponse<>(downloadLink));
    }

    /**
     * Активация цифрового продукта по коду активации
     * 
     * @param code код активации
     * @param request данные для активации
     * @param user аутентифицированный пользователь
     * @return результат активации
     */
    @PostMapping("/activate/{code}")
    public ResponseEntity<ApiResponse<DesktopProductActivationResponseDto>> activateProduct(
            @PathVariable String code,
            @Valid @RequestBody ProductActivationRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Активация продукта с кодом: {} для пользователя: {}", code, user.getUsername());
        
        DesktopProductActivationResponseDto result = desktopProductService.activateProduct(code, request, user);
        
        return ResponseEntity.ok(new ApiResponse<>(result));
    }
} 