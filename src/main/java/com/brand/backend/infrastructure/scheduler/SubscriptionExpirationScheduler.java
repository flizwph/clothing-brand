package com.brand.backend.infrastructure.scheduler;

import com.brand.backend.application.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Планировщик для проверки и обновления статуса подписок
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpirationScheduler {

    private final SubscriptionService subscriptionService;

    /**
     * Запускается каждый день в 00:00 для обновления статуса просроченных подписок
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkExpiredSubscriptions() {
        try {
            subscriptionService.deactivateExpiredSubscriptions();
            log.info("Проверка истекших подписок завершена");
        } catch (Exception e) {
            log.error("Ошибка при проверке истекших подписок: {}", e.getMessage(), e);
        }
    }
} 