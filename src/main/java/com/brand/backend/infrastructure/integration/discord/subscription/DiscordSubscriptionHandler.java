package com.brand.backend.infrastructure.integration.discord.subscription;

import com.brand.backend.application.subscription.service.SubscriptionService;
import com.brand.backend.domain.subscription.model.PurchasePlatform;
import com.brand.backend.domain.subscription.model.Subscription;
import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Обработчик подписок для Discord-бота
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordSubscriptionHandler {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    
    /**
     * Создает подписку для пользователя Discord
     * @param discordId Discord ID пользователя
     * @param level уровень подписки
     * @return строка с информацией о созданной подписке
     */
    public String createSubscription(Long discordId, SubscriptionLevel level) {
        try {
            Optional<User> userOptional = userRepository.findByDiscordId(discordId);
            if (userOptional.isEmpty()) {
                return "Вы не зарегистрированы в системе. Пожалуйста, сначала зарегистрируйтесь.";
            }
            
            User user = userOptional.get();
            int durationInDays = getDurationForLevel(level);
            
            Subscription subscription = subscriptionService.createSubscription(
                    user.getId(),
                    level,
                    durationInDays,
                    PurchasePlatform.DISCORD
            );
            
            return String.format("""
                    Ваша подписка уровня **%s** успешно создана!
                    
                    Код активации: `%s`
                    
                    Сохраните этот код, он понадобится для активации десктоп-приложения.
                    """, 
                    getSubscriptionLevelName(level),
                    subscription.getActivationCode()
            );
        } catch (Exception e) {
            log.error("Ошибка при создании подписки для Discord пользователя {}: {}", discordId, e.getMessage());
            return "Произошла ошибка при создании подписки. Пожалуйста, попробуйте позже.";
        }
    }
    
    /**
     * Отображает информацию о подписках пользователя
     * @param discordId Discord ID пользователя
     * @return строка с информацией о подписках
     */
    public String getSubscriptions(Long discordId) {
        try {
            Optional<User> userOptional = userRepository.findByDiscordId(discordId);
            if (userOptional.isEmpty()) {
                return "Вы не зарегистрированы в системе. Пожалуйста, сначала зарегистрируйтесь.";
            }
            
            User user = userOptional.get();
            var subscriptions = subscriptionService.getUserActiveSubscriptions(user.getId());
            
            if (subscriptions.isEmpty()) {
                return "У вас нет активных подписок.";
            }
            
            StringBuilder message = new StringBuilder("Ваши активные подписки:\n\n");
            
            for (Subscription subscription : subscriptions) {
                message.append("Уровень: **")
                        .append(getSubscriptionLevelName(subscription.getSubscriptionLevel()))
                        .append("**\n")
                        .append("Код активации: `")
                        .append(subscription.getActivationCode())
                        .append("`\n");
                
                if (subscription.getStartDate() != null) {
                    message.append("Начало: **")
                            .append(subscription.getStartDate().format(DATE_FORMATTER))
                            .append("**\n");
                }
                
                if (subscription.getEndDate() != null) {
                    message.append("Окончание: **")
                            .append(subscription.getEndDate().format(DATE_FORMATTER))
                            .append("**\n");
                }
                
                message.append("Статус: **")
                        .append(subscription.isActive() ? "Активна" : "Не активирована")
                        .append("**\n\n");
            }
            
            return message.toString();
        } catch (Exception e) {
            log.error("Ошибка при получении подписок для Discord пользователя {}: {}", discordId, e.getMessage());
            return "Произошла ошибка при получении информации о подписках. Пожалуйста, попробуйте позже.";
        }
    }
    
    /**
     * Активирует подписку по коду активации
     * @param activationCode код активации
     * @return строка с результатом активации
     */
    public String activateSubscription(String activationCode) {
        try {
            Subscription subscription = subscriptionService.activateSubscription(activationCode);
            
            return String.format("""
                    Подписка успешно активирована!
                    
                    Уровень: **%s**
                    Действует до: **%s**
                    """,
                    getSubscriptionLevelName(subscription.getSubscriptionLevel()),
                    subscription.getEndDate().format(DATE_FORMATTER)
            );
        } catch (Exception e) {
            log.error("Ошибка при активации подписки с кодом {}: {}", activationCode, e.getMessage());
            return "Ошибка активации: " + e.getMessage();
        }
    }
    
    /**
     * Возвращает название уровня подписки
     * @param level уровень подписки
     * @return название уровня подписки
     */
    private String getSubscriptionLevelName(SubscriptionLevel level) {
        return switch (level) {
            case BASIC -> "Базовый";
            case STANDARD -> "Стандартный";
            case PREMIUM -> "Премиум";
        };
    }
    
    /**
     * Определяет длительность подписки в днях в зависимости от уровня
     * @param level уровень подписки
     * @return длительность подписки в днях
     */
    private int getDurationForLevel(SubscriptionLevel level) {
        return switch (level) {
            case BASIC -> 30;    // 1 месяц
            case STANDARD -> 90; // 3 месяца
            case PREMIUM -> 365; // 1 год
        };
    }
} 