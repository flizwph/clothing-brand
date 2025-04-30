package com.brand.backend.presentation.rest.controller.subscription;

import com.brand.backend.application.subscription.service.SubscriptionService;
import com.brand.backend.common.exeption.ActivationCodeNotFoundException;
import com.brand.backend.domain.subscription.model.Subscription;
import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import com.brand.backend.domain.subscription.repository.SubscriptionRepository;
import com.brand.backend.presentation.dto.request.subscription.ActivateSubscriptionRequest;
import com.brand.backend.presentation.dto.response.ApiResponse;
import com.brand.backend.presentation.dto.response.subscription.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

/**
 * Контроллер для работы с десктопным приложением
 */
@RestController
@RequestMapping("/api/desktop")
@RequiredArgsConstructor
public class DesktopSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Активирует подписку по коду активации
     * @param request запрос с кодом активации
     * @return информация об активированной подписке
     */
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> activateSubscription(
            @Valid @RequestBody ActivateSubscriptionRequest request) {
        
        Subscription subscription = subscriptionService.activateSubscription(request.getActivationCode());
        
        SubscriptionResponse response = SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .activationCode(subscription.getActivationCode())
                .subscriptionLevel(subscription.getSubscriptionLevel())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .isActive(subscription.isActive())
                .purchasePlatform(subscription.getPurchasePlatform())
                .build();
        
        return ResponseEntity.ok(new ApiResponse<>(response));
    }

    /**
     * Проверяет статус подписки
     * @param activationCode код активации
     * @return информация о статусе подписки
     */
    @GetMapping("/check/{activationCode}")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> checkSubscriptionStatus(
            @PathVariable String activationCode) {
        
        try {
            Subscription subscription = subscriptionRepository.findByActivationCode(activationCode)
                    .orElseThrow(() -> new ActivationCodeNotFoundException("Код активации не найден"));
            
            boolean isActive = subscription.isActive() && 
                              LocalDateTime.now().isBefore(subscription.getEndDate());
            
            SubscriptionStatusResponse response = SubscriptionStatusResponse.builder()
                    .isActive(isActive)
                    .level(subscription.getSubscriptionLevel())
                    .expirationDate(subscription.getEndDate())
                    .build();
            
            // Обновляем дату последней проверки
            subscription.setLastCheckDate(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            return ResponseEntity.ok(new ApiResponse<>(response));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>(
                    SubscriptionStatusResponse.builder()
                            .isActive(false)
                            .level(null)
                            .errorMessage(e.getMessage())
                            .build()
            ));
        }
    }

    /**
     * Класс для ответа о статусе подписки
     */
    @lombok.Data
    @lombok.Builder
    public static class SubscriptionStatusResponse {
        private boolean isActive;
        private SubscriptionLevel level;
        private LocalDateTime expirationDate;
        private String errorMessage;
    }
} 