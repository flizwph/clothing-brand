package com.brand.backend.presentation.rest.controller.subscription;

import com.brand.backend.application.subscription.service.SubscriptionService;
import com.brand.backend.domain.subscription.model.Subscription;
import com.brand.backend.domain.subscription.model.SubscriptionType;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/desktop/subscription")
@RequiredArgsConstructor
@Slf4j
public class DesktopSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> activateSubscription(
            @RequestBody ActivationRequest request,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        try {
            subscriptionService.activateSubscription(user.getId(), request.getActivationCode());
            
            user.setLastCheckTime(LocalDateTime.now());
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Подписка успешно активирована"
            ));
        } catch (Exception e) {
            log.error("Ошибка активации подписки: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkSubscription(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Subscription> activeSubscriptions = subscriptionService.findActiveSubscriptionsByUserId(user.getId());

        if (activeSubscriptions.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "hasAccess", false,
                "message", "Нет активных подписок"
            ));
        }

        Subscription bestSubscription = activeSubscriptions.stream()
                .max(Comparator.comparing(s -> getSubscriptionLevel(s.getType())))
                .orElse(null);

        if (bestSubscription != null && bestSubscription.getExpirationDate().isAfter(LocalDateTime.now())) {
            user.setLastCheckTime(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "hasAccess", true,
                "subscriptionType", bestSubscription.getType(),
                "expirationDate", bestSubscription.getExpirationDate(),
                "message", "Доступ разрешен"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "hasAccess", false,
            "message", "Подписка истекла"
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Subscription> userSubscriptions = subscriptionService.getUserSubscriptions(user.getId());
        
        return ResponseEntity.ok(Map.of(
            "subscriptions", userSubscriptions,
            "totalCount", userSubscriptions.size()
        ));
    }

    private int getSubscriptionLevel(SubscriptionType type) {
        return switch (type) {
            case VIP -> 3;
            case ENTERPRISE -> 3;
            case PREMIUM -> 2;
            case BASIC -> 1;
        };
    }

    public static class ActivationRequest {
        private String activationCode;

        public String getActivationCode() { return activationCode; }
        public void setActivationCode(String activationCode) { this.activationCode = activationCode; }
    }
} 