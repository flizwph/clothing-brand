package com.brand.backend.application.subscription.service;

import com.brand.backend.common.exeption.ActivationCodeNotFoundException;
import com.brand.backend.common.exeption.SubscriptionAlreadyActiveException;
import com.brand.backend.common.exeption.UserNotFoundException;
import com.brand.backend.domain.subscription.model.PurchasePlatform;
import com.brand.backend.domain.subscription.model.Subscription;
import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import com.brand.backend.domain.subscription.repository.SubscriptionRepository;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.brand.backend.domain.subscription.model.SubscriptionActivationResult;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Генерирует уникальный код активации
     * @return сгенерированный код активации
     */
    public String generateActivationCode() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        // Проверяем, что код уникален
        while (subscriptionRepository.existsByActivationCode(code)) {
            secureRandom.nextBytes(randomBytes);
            code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        }
        
        return code;
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
            case BASIC -> 30;    // 1 месяц
            case STANDARD -> 90; // 3 месяца
            case PREMIUM -> 365; // 1 год
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
} 