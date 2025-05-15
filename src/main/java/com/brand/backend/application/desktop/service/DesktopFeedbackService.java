package com.brand.backend.application.desktop.service;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.AppRatingRequest;
import com.brand.backend.presentation.dto.request.desktop.FeedbackRequest;
import com.brand.backend.presentation.dto.request.desktop.ProductReviewRequest;
import com.brand.backend.presentation.dto.response.desktop.AppRatingResponseDto;
import com.brand.backend.presentation.dto.response.desktop.FeedbackResponseDto;
import com.brand.backend.presentation.dto.response.desktop.ProductReviewResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Сервис для управления отзывами и обратной связью в десктопном приложении
 */
public interface DesktopFeedbackService {
    
    /**
     * Отправляет отзыв о приложении
     * 
     * @param request данные отзыва
     * @param user пользователь
     * @return результат отправки отзыва
     */
    FeedbackResponseDto submitFeedback(FeedbackRequest request, User user);
    
    /**
     * Добавляет отзыв о цифровом продукте
     * 
     * @param productId идентификатор продукта
     * @param request данные отзыва
     * @param user пользователь
     * @return результат добавления отзыва
     */
    ProductReviewResponseDto submitProductReview(Long productId, ProductReviewRequest request, User user);
    
    /**
     * Получает список отзывов о продукте
     * 
     * @param productId идентификатор продукта
     * @param pageable параметры пагинации
     * @return список отзывов
     */
    Page<ProductReviewResponseDto> getProductReviews(Long productId, Pageable pageable);
    
    /**
     * Сохраняет оценку десктопного приложения
     * 
     * @param request данные оценки
     * @param user пользователь
     * @return результат оценки
     */
    AppRatingResponseDto rateApplication(AppRatingRequest request, User user);
} 