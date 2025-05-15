package com.brand.backend.presentation.rest.controller.desktop;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.FeedbackRequest;
import com.brand.backend.presentation.dto.request.desktop.ProductReviewRequest;
import com.brand.backend.presentation.dto.request.desktop.AppRatingRequest;
import com.brand.backend.presentation.dto.response.ApiResponse;
import com.brand.backend.presentation.dto.response.desktop.FeedbackResponseDto;
import com.brand.backend.presentation.dto.response.desktop.ProductReviewResponseDto;
import com.brand.backend.presentation.dto.response.desktop.AppRatingResponseDto;
import com.brand.backend.application.desktop.service.DesktopFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Контроллер для работы с отзывами и обратной связью в десктопном приложении
 */
@RestController
@RequestMapping("/api/desktop/feedback")
@RequiredArgsConstructor
@Slf4j
public class DesktopFeedbackController {

    private final DesktopFeedbackService feedbackService;

    /**
     * Отправка отзыва о приложении
     * 
     * @param request данные отзыва
     * @param user аутентифицированный пользователь
     * @return результат отправки отзыва
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponseDto>> submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Отправка отзыва от пользователя: {}, тип: {}", 
                user.getUsername(), request.getType());
        
        FeedbackResponseDto result = feedbackService.submitFeedback(request, user);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(result));
    }

    /**
     * Добавление отзыва о цифровом продукте
     * 
     * @param productId идентификатор продукта
     * @param request данные отзыва
     * @param user аутентифицированный пользователь
     * @return результат добавления отзыва
     */
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<ProductReviewResponseDto>> submitProductReview(
            @PathVariable Long productId,
            @Valid @RequestBody ProductReviewRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Добавление отзыва о продукте ID: {} от пользователя: {}, рейтинг: {}", 
                productId, user.getUsername(), request.getRating());
        
        ProductReviewResponseDto result = feedbackService.submitProductReview(productId, request, user);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(result));
    }

    /**
     * Получение списка отзывов о продукте
     * 
     * @param productId идентификатор продукта
     * @param pageable параметры пагинации
     * @return список отзывов
     */
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<Page<ProductReviewResponseDto>>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 10) Pageable pageable) {
        
        log.info("Получение списка отзывов о продукте ID: {}", productId);
        
        Page<ProductReviewResponseDto> reviews = feedbackService.getProductReviews(productId, pageable);
        
        return ResponseEntity.ok(new ApiResponse<>(reviews));
    }

    /**
     * Оценка десктопного приложения
     * 
     * @param request данные оценки
     * @param user аутентифицированный пользователь
     * @return результат оценки
     */
    @PostMapping("/rating")
    public ResponseEntity<ApiResponse<AppRatingResponseDto>> rateApplication(
            @Valid @RequestBody AppRatingRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Оценка приложения от пользователя: {}, рейтинг: {}", 
                user.getUsername(), request.getRating());
        
        AppRatingResponseDto result = feedbackService.rateApplication(request, user);
        
        return ResponseEntity.ok(new ApiResponse<>(result));
    }
} 