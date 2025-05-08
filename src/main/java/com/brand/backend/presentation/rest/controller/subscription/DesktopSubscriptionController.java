package com.brand.backend.presentation.rest.controller.subscription;

import com.brand.backend.application.subscription.service.SubscriptionService;
import com.brand.backend.common.exeption.ActivationCodeNotFoundException;
import com.brand.backend.domain.subscription.model.Subscription;
import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import com.brand.backend.domain.subscription.model.SubscriptionStatus;
import com.brand.backend.domain.subscription.repository.SubscriptionRepository;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.subscription.ActivateSubscriptionRequest;
import com.brand.backend.presentation.dto.response.ApiResponse;
import com.brand.backend.presentation.dto.response.subscription.DesktopSubscriptionStatusResponse;
import com.brand.backend.presentation.dto.response.subscription.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
     * Проверяет статус подписки для авторизованного пользователя
     * @param user аутентифицированный пользователь
     * @return статус подписки desktop-приложения
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<DesktopSubscriptionStatusResponse>> getSubscriptionStatus(
            @AuthenticationPrincipal User user) {
        
        LocalDateTime now = LocalDateTime.now();
        // Получаем все действующие подписки пользователя
        var subscriptions = subscriptionService.getUserValidSubscriptions(user.getId());
        
        if (subscriptions.isEmpty()) {
            // Если нет активных подписок
            return ResponseEntity.ok(new ApiResponse<>(
                    DesktopSubscriptionStatusResponse.builder()
                            .status(SubscriptionStatus.INACTIVE)
                            .build()
            ));
        }
        
        // Находим подписку с самым высоким уровнем среди активных
        Subscription highestLevelSubscription = subscriptions.stream()
                .sorted((s1, s2) -> {
                    // Сортировка по уровню подписки (PREMIUM > STANDARD > BASIC)
                    return s2.getSubscriptionLevel().ordinal() - s1.getSubscriptionLevel().ordinal();
                })
                .findFirst()
                .orElse(null);
        
        if (highestLevelSubscription != null) {
            // Проверяем срок действия подписки
            boolean isExpired = now.isAfter(highestLevelSubscription.getEndDate());
            
            SubscriptionStatus status = isExpired ? 
                    SubscriptionStatus.EXPIRED : 
                    (highestLevelSubscription.isActive() ? SubscriptionStatus.ACTIVE : SubscriptionStatus.PENDING);
            
            DesktopSubscriptionStatusResponse response = DesktopSubscriptionStatusResponse.builder()
                    .status(status)
                    .level(highestLevelSubscription.getSubscriptionLevel())
                    .activationDate(highestLevelSubscription.getStartDate())
                    .expirationDate(highestLevelSubscription.getEndDate())
                    .build();
            
            // Обновляем дату последней проверки
            highestLevelSubscription.setLastCheckDate(now);
            subscriptionRepository.save(highestLevelSubscription);
            
            return ResponseEntity.ok(new ApiResponse<>(response));
        }
        
        // Если не нашли подходящую подписку
        return ResponseEntity.ok(new ApiResponse<>(
                DesktopSubscriptionStatusResponse.builder()
                        .status(SubscriptionStatus.INACTIVE)
                        .build()
        ));
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