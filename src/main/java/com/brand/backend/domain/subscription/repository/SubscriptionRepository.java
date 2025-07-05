package com.brand.backend.domain.subscription.repository;

import com.brand.backend.domain.subscription.model.Subscription;
import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import com.brand.backend.domain.subscription.model.SubscriptionType;
import com.brand.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    Optional<Subscription> findByActivationCode(String activationCode);
    
    /**
     * Находит все подписки пользователя, включая активные и неактивные
     * @param user пользователь
     * @return список всех подписок пользователя
     */
    List<Subscription> findByUser(User user);
    
    /**
     * Находит все активные подписки пользователя
     * @param user пользователь
     * @return список активных подписок
     */
    List<Subscription> findByUserAndIsActiveTrue(User user);
    
    /**
     * Находит активные подписки пользователя, срок действия которых не истек
     * @param user пользователь
     * @param currentDate текущая дата
     * @return список действующих подписок
     */
    @Query("SELECT s FROM Subscription s WHERE s.user = :user AND s.isActive = true AND s.endDate > :currentDate")
    List<Subscription> findValidSubscriptions(@Param("user") User user, @Param("currentDate") LocalDateTime currentDate);
    
    /**
     * Находит подписку по пользователю и уровню подписки
     * @param user пользователь
     * @param subscriptionLevel уровень подписки
     * @return опциональная подписка
     */
    Optional<Subscription> findByUserAndSubscriptionLevel(User user, SubscriptionLevel subscriptionLevel);
    
    @Query("SELECT s FROM Subscription s WHERE s.isActive = true AND s.endDate < :currentDate")
    List<Subscription> findExpiredSubscriptions(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT s FROM Subscription s WHERE s.isActive = true AND s.endDate BETWEEN :startDate AND :endDate")
    List<Subscription> findSubscriptionsExpiringBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    boolean existsByActivationCode(String activationCode);

    // New methods for compatibility
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId")
    List<Subscription> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.isActive = true")
    List<Subscription> findActiveSubscriptionsByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Subscription s WHERE s.isActive = true")
    List<Subscription> findAllActiveSubscriptions();

    @Query("SELECT s FROM Subscription s WHERE s.isActive = true AND s.endDate < :currentDate")
    List<Subscription> findExpiredActiveSubscriptions(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.user.id = :userId AND (s.type = :type OR s.subscriptionLevel = :level) AND s.isActive = true AND s.endDate > :currentDate")
    boolean hasActiveSubscription(@Param("userId") Long userId, @Param("type") SubscriptionType type, @Param("level") SubscriptionLevel level);

    // Overloaded method for just SubscriptionType
    default boolean hasActiveSubscription(Long userId, SubscriptionType type) {
        SubscriptionLevel level = convertTypeToLevel(type);
        return hasActiveSubscription(userId, type, level);
    }

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.isActive = true")
    Long countActiveSubscriptions();

    private static SubscriptionLevel convertTypeToLevel(SubscriptionType type) {
        return switch (type) {
            case BASIC -> SubscriptionLevel.STANDARD;
            case PREMIUM -> SubscriptionLevel.PREMIUM;
            case VIP -> SubscriptionLevel.DELUXE;
            case ENTERPRISE -> SubscriptionLevel.DELUXE; // fallback
        };
    }
} 