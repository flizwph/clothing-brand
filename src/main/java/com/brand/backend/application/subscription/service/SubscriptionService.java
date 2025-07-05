package com.brand.backend.application.subscription.service;

import com.brand.backend.common.exception.ActivationCodeNotFoundException;
import com.brand.backend.common.exception.SubscriptionAlreadyActiveException;
import com.brand.backend.common.exception.UserNotFoundException;
import com.brand.backend.domain.subscription.model.PurchasePlatform;
import com.brand.backend.domain.subscription.model.Subscription;
import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import com.brand.backend.domain.subscription.repository.SubscriptionRepository;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.presentation.dto.response.SubscriptionInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.brand.backend.domain.subscription.model.SubscriptionActivationResult;
import com.brand.backend.domain.subscription.model.SubscriptionPlan;
import com.brand.backend.domain.subscription.repository.SubscriptionPlanRepository;
import com.brand.backend.presentation.dto.response.SubscriptionPlanDTO;
import com.brand.backend.presentation.dto.response.SubscriptionLimitsDTO;
import com.brand.backend.presentation.dto.response.SubscriptionUsageDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.util.UUID;

import com.brand.backend.domain.subscription.exception.SubscriptionNotFoundException;
import com.brand.backend.domain.subscription.model.SubscriptionType;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ObjectMapper objectMapper;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Генерирует уникальный код активации
     * @return сгенерированный код активации
     */
    public String generateActivationCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    /**
     * Создает новую подписку для пользователя
     * @param userId ID пользователя
     * @param level уровень подписки
     * @param durationInDays длительность подписки в днях
     * @param platform платформа, с которой была приобретена подписка
     * @return объект созданной подписки
     */
    @Transactional
    public Subscription createSubscription(Long userId, SubscriptionLevel level, int durationInDays, PurchasePlatform platform) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));
        
        String activationCode = generateActivationCode();
        
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setActivationCode(activationCode);
        subscription.setSubscriptionLevel(level);
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setPurchasePlatform(platform);
        
        return subscriptionRepository.save(subscription);
    }

    /**
     * Активирует подписку по коду активации и привязывает к пользователю
     * @param userId ID пользователя
     * @param activationCode код активации
     * @return результат активации подписки
     */
    @Transactional
    public SubscriptionActivationResult activateByCode(Long userId, String activationCode) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));
            
            Subscription subscription = subscriptionRepository.findByActivationCode(activationCode)
                    .orElseThrow(() -> new ActivationCodeNotFoundException("Код активации " + activationCode + " не найден"));
            
            if (subscription.isActive()) {
                return SubscriptionActivationResult.error("Подписка уже активирована");
            }
            
            LocalDateTime now = LocalDateTime.now();
            
            // Определяем длительность подписки
            int durationInDays = getDurationForLevel(subscription.getSubscriptionLevel());
            
            // Привязываем подписку к пользователю
            subscription.setUser(user);
            subscription.setStartDate(now);
            subscription.setEndDate(now.plusDays(durationInDays));
            subscription.setActive(true);
            subscription.setUpdatedAt(now);
            subscription.setLastCheckDate(now);
            
            subscription = subscriptionRepository.save(subscription);
            
            return SubscriptionActivationResult.success(subscription);
        } catch (ActivationCodeNotFoundException e) {
            return SubscriptionActivationResult.error("Код активации не найден");
        } catch (UserNotFoundException e) {
            return SubscriptionActivationResult.error("Пользователь не найден");
        } catch (Exception e) {
            return SubscriptionActivationResult.error("Произошла ошибка при активации подписки: " + e.getMessage());
        }
    }
    
    /**
     * Проверяет статус подписки
     * @param userId ID пользователя
     * @param level уровень подписки
     * @return true, если подписка активна, иначе false
     */
    public boolean isSubscriptionActive(Long userId, SubscriptionLevel level) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));
        
        Optional<Subscription> subscription = subscriptionRepository.findByUserAndSubscriptionLevel(user, level);
        
        return subscription.isPresent() && 
               subscription.get().isActive() && 
               LocalDateTime.now().isBefore(subscription.get().getEndDate());
    }
    
    /**
     * Возвращает все активные подписки пользователя
     * @param userId ID пользователя
     * @return список активных подписок
     */
    public List<Subscription> getUserActiveSubscriptions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));
        
        return subscriptionRepository.findByUserAndIsActiveTrue(user);
    }
    
    /**
     * Возвращает все действующие подписки пользователя (активные и не просроченные)
     * @param userId ID пользователя
     * @return список действующих подписок
     */
    public List<Subscription> getUserValidSubscriptions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));
        
        LocalDateTime now = LocalDateTime.now();
        return subscriptionRepository.findValidSubscriptions(user, now);
    }
    
    /**
     * Обновляет статус просроченных подписок
     */
    @Transactional
    public void updateExpiredSubscriptions() {
        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions(LocalDateTime.now());
        
        for (Subscription subscription : expiredSubscriptions) {
            subscription.setActive(false);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
        }
    }
    
    /**
     * Определяет длительность подписки в днях в зависимости от уровня
     * @param level уровень подписки
     * @return длительность подписки в днях
     */
    private int getDurationForLevel(SubscriptionLevel level) {
        return switch (level) {
            case STANDARD -> 30;  // 1 месяц
            case PREMIUM -> 90;   // 3 месяца
            case DELUXE -> 365;   // 1 год
        };
    }

    /**
     * Обновляет существующую подписку
     * @param subscription подписка для обновления
     * @return обновленная подписка
     */
    @Transactional
    public Subscription updateSubscription(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    /**
     * Возвращает все подписки пользователя, включая неактивные и просроченные
     * @param userId ID пользователя
     * @return список всех подписок
     */
    public List<Subscription> getAllSubscriptions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));
        
        return subscriptionRepository.findByUser(user);
    }
    
    /**
     * Получает подписку по её ID
     * @param subscriptionId ID подписки
     * @return опциональная подписка
     */
    public Optional<Subscription> getSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId);
    }
    
    /**
     * Активирует подписку по коду активации
     * @param activationCode код активации
     * @return активированная подписка
     * @throws ActivationCodeNotFoundException если код активации не найден
     * @throws SubscriptionAlreadyActiveException если подписка уже активирована
     */
    @Transactional
    public Subscription activateSubscription(String activationCode) {
        Subscription subscription = subscriptionRepository.findByActivationCode(activationCode)
                .orElseThrow(() -> new ActivationCodeNotFoundException("Код активации " + activationCode + " не найден"));
        
        if (subscription.isActive()) {
            throw new SubscriptionAlreadyActiveException("Подписка уже активирована");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Определяем длительность подписки
        int durationInDays = getDurationForLevel(subscription.getSubscriptionLevel());
        
        // Активируем подписку
        subscription.setStartDate(now);
        subscription.setEndDate(now.plusDays(durationInDays));
        subscription.setActive(true);
        subscription.setUpdatedAt(now);
        subscription.setLastCheckDate(now);
        
        return subscriptionRepository.save(subscription);
    }

    /**
     * Получение детальной информации о подписке с лимитами и использованием
     */
    public SubscriptionInfoDTO getDetailedSubscriptionInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndIsActiveTrue(user);
        
        if (activeSubscriptions.isEmpty()) {
            return createEmptySubscriptionInfo();
        }

        Subscription subscription = activeSubscriptions.get(0);
        SubscriptionPlan plan = subscriptionPlanRepository.findByLevel(subscription.getSubscriptionLevel())
                .orElse(null);

        // Рассчитываем оставшиеся дни
        int daysRemaining = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), subscription.getEndDate());
        if (daysRemaining < 0) daysRemaining = 0;

        // Следующее списание (если автопродление включено)
        LocalDateTime nextBillingDate = subscription.isAutoRenewal() ? subscription.getEndDate() : null;

        // Лимиты из плана
        SubscriptionLimitsDTO limits = plan != null ? 
                new SubscriptionLimitsDTO(plan.getInstrumentsLimit(), plan.getWalletsLimit(), plan.getDevicesLimit()) :
                new SubscriptionLimitsDTO(0, 0, 0);

        // Текущее использование (пока заглушка)
        SubscriptionUsageDTO usage = new SubscriptionUsageDTO(3, 2, 1); // TODO: реальные данные

        return new SubscriptionInfoDTO(
                subscription.getSubscriptionLevel().name(),
                subscription.isActive(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                plan != null ? plan.getPrice() : getSubscriptionPrice(subscription.getSubscriptionLevel().name()),
                getSubscriptionName(subscription.getSubscriptionLevel().name()),
                subscription.isAutoRenewal(),
                daysRemaining,
                nextBillingDate,
                limits,
                usage
        );
    }

    /**
     * Получение всех активных тарифных планов
     */
    public List<SubscriptionPlanDTO> getAllActivePlans() {
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findByIsActiveTrueOrderByPriceAsc();
        
        return plans.stream()
                .map(this::mapPlanToDTO)
                .toList();
    }

    /**
     * Продление подписки
     */
    @Transactional
    public Map<String, Object> renewSubscription(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndIsActiveTrue(user);
        
        if (activeSubscriptions.isEmpty()) {
            throw new RuntimeException("Нет активной подписки для продления");
        }

        Subscription subscription = activeSubscriptions.get(0);
        
        // TODO: Здесь должна быть интеграция с платежной системой
        // Пока что просто продлеваем на месяц
        subscription.setEndDate(subscription.getEndDate().plusDays(30));
        subscriptionRepository.save(subscription);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Перенаправление на оплату");
        result.put("paymentUrl", "/payment?action=renew&subscription=" + subscription.getId());
        result.put("amount", getSubscriptionPrice(subscription.getSubscriptionLevel().name()));
        
        log.info("Подписка {} продлена для пользователя {}", subscription.getSubscriptionLevel(), user.getUsername());
        
        return result;
    }

    /**
     * Смена тарифного плана
     */
    @Transactional
    public Map<String, Object> changeSubscriptionPlan(Long userId, String level) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        SubscriptionLevel newLevel = SubscriptionLevel.valueOf(level.toUpperCase());
        SubscriptionPlan newPlan = subscriptionPlanRepository.findByLevelAndIsActiveTrue(newLevel)
                .orElseThrow(() -> new RuntimeException("Тарифный план не найден"));

        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndIsActiveTrue(user);
        
        if (!activeSubscriptions.isEmpty()) {
            Subscription currentSubscription = activeSubscriptions.get(0);
            currentSubscription.setSubscriptionLevel(newLevel);
            subscriptionRepository.save(currentSubscription);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Перенаправление на оплату");
        result.put("paymentUrl", "/payment?action=change&plan=" + newLevel);
        result.put("newPlan", newPlan.getName());
        result.put("amount", newPlan.getPrice());
        
        log.info("План подписки изменен на {} для пользователя {}", newLevel, user.getUsername());
        
        return result;
    }

    /**
     * Переключение автопродления
     */
    @Transactional
    public Map<String, Object> toggleAutoRenewal(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndIsActiveTrue(user);
        
        if (activeSubscriptions.isEmpty()) {
            throw new RuntimeException("Нет активной подписки");
        }

        Subscription subscription = activeSubscriptions.get(0);
        subscription.setAutoRenewal(enabled);
        subscriptionRepository.save(subscription);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("autoRenewal", enabled);
        result.put("message", enabled ? "Автопродление включено" : "Автопродление отключено");
        
        if (enabled) {
            result.put("nextBilling", subscription.getEndDate());
            result.put("amount", getSubscriptionPrice(subscription.getSubscriptionLevel().name()));
        }
        
        log.info("Автопродление {} для пользователя {}", enabled ? "включено" : "отключено", user.getUsername());
        
        return result;
    }

    /**
     * Отмена подписки
     */
    @Transactional
    public Map<String, Object> cancelSubscription(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndIsActiveTrue(user);
        
        if (activeSubscriptions.isEmpty()) {
            throw new RuntimeException("Нет активной подписки для отмены");
        }

        Subscription subscription = activeSubscriptions.get(0);
        subscription.setAutoRenewal(false);
        // Подписка остается активной до окончания оплаченного периода
        subscriptionRepository.save(subscription);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Подписка будет отменена в конце оплаченного периода");
        result.put("activeUntil", subscription.getEndDate());
        
        log.info("Подписка отменена для пользователя {}, активна до {}", user.getUsername(), subscription.getEndDate());
        
        return result;
    }

    /**
     * Создание пустой информации о подписке
     */
    private SubscriptionInfoDTO createEmptySubscriptionInfo() {
        return new SubscriptionInfoDTO(
                "NONE",
                false,
                null,
                null,
                BigDecimal.ZERO,
                "Нет активной подписки",
                false,
                0,
                null,
                new SubscriptionLimitsDTO(0, 0, 0),
                new SubscriptionUsageDTO(0, 0, 0)
        );
    }

    /**
     * Маппинг SubscriptionPlan в DTO
     */
    private SubscriptionPlanDTO mapPlanToDTO(SubscriptionPlan plan) {
        List<String> features = parseFeatures(plan.getFeatures());
        boolean isRecommended = plan.getLevel() == SubscriptionLevel.STANDARD; // STANDARD как рекомендуемый
        
        return new SubscriptionPlanDTO(
                plan.getId(),
                plan.getLevel().name(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getDurationDays(),
                plan.getInstrumentsLimit(),
                plan.getWalletsLimit(),
                plan.getDevicesLimit(),
                features,
                plan.isActive(),
                isRecommended
        );
    }

    /**
     * Парсинг JSON строки с функциями
     */
    private List<String> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.isEmpty()) {
            return Arrays.asList("Базовый функционал");
        }
        
        try {
            return objectMapper.readValue(featuresJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Arrays.asList("Базовый функционал");
        }
    }

    /**
     * Вспомогательный метод для получения цены подписки
     */
    private BigDecimal getSubscriptionPrice(String level) {
        return switch (level) {
            case "STANDARD" -> new BigDecimal("299");
            case "PREMIUM" -> new BigDecimal("599");
            case "DELUXE" -> new BigDecimal("999");
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Вспомогательный метод для получения названия подписки
     */
    private String getSubscriptionName(String level) {
        return switch (level) {
            case "STANDARD" -> "Стандартная подписка";
            case "PREMIUM" -> "Премиум подписка";
            case "DELUXE" -> "Делюкс подписка";
            default -> "Неизвестная подписка";
        };
    }

    public Subscription createSubscription(SubscriptionType type, int durationInDays) {
        Subscription subscription = new Subscription();
        subscription.setType(type);
        subscription.setActive(false);
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setExpirationDate(LocalDateTime.now().plusDays(durationInDays));
        subscription.setActivationCode(generateActivationCode());
        subscription.setAutoRenewal(false);
        
        return subscriptionRepository.save(subscription);
    }

    public void activateSubscription(Long userId, String activationCode) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByActivationCode(activationCode);
        
        if (subscriptionOpt.isEmpty()) {
            throw new SubscriptionNotFoundException("Подписка с кодом " + activationCode + " не найдена");
        }
        
        Subscription subscription = subscriptionOpt.get();
        
        if (subscription.isActive()) {
            throw new IllegalStateException("Подписка уже активирована");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        subscription.setUser(user);
        subscription.setActive(true);
        subscription.setActivatedAt(LocalDateTime.now());
        
        subscriptionRepository.save(subscription);
        log.info("Подписка {} активирована для пользователя {}", subscription.getId(), userId);
    }

    public List<Subscription> findActiveSubscriptionsByUserId(Long userId) {
        return subscriptionRepository.findActiveSubscriptionsByUserId(userId);
    }

    public List<Subscription> findAllActiveSubscriptions() {
        return subscriptionRepository.findAllActiveSubscriptions();
    }

    public Optional<Subscription> findByActivationCode(String activationCode) {
        return subscriptionRepository.findByActivationCode(activationCode);
    }

    public Subscription save(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    public void deactivateExpiredSubscriptions() {
        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredActiveSubscriptions(LocalDateTime.now());
        
        for (Subscription subscription : expiredSubscriptions) {
            subscription.setActive(false);
            subscriptionRepository.save(subscription);
            log.info("Подписка {} деактивирована по истечении срока", subscription.getId());
        }
    }

    public boolean hasActiveSubscription(Long userId, SubscriptionType type) {
        return subscriptionRepository.hasActiveSubscription(userId, type);
    }

    public void extendSubscription(Long subscriptionId, int additionalDays) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Подписка не найдена"));
        
        subscription.setExpirationDate(subscription.getExpirationDate().plusDays(additionalDays));
        subscriptionRepository.save(subscription);
        
        log.info("Подписка {} продлена на {} дней", subscriptionId, additionalDays);
    }

    public void cancelSubscriptionById(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Подписка не найдена"));
        
        subscription.setActive(false);
        subscription.setAutoRenewal(false);
        subscriptionRepository.save(subscription);
        
        log.info("Подписка {} отменена", subscriptionId);
    }

    public List<Subscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public long getActiveSubscriptionsCount() {
        return subscriptionRepository.countActiveSubscriptions();
    }
} 