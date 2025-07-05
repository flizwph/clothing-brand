package com.brand.backend.domain.user.repository;

import com.brand.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByTelegramId(Long telegramId);
    Optional<User> findByTelegramUsername(String telegramUsername);
    Optional<User> findByVerificationCode(String verificationCode);
    Optional<User> findByLastUsedVerificationCode(String lastUsedVerificationCode);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByDiscordId(Long discordId);
    Optional<User> findByVkUsername(String vkUsername);
    

    
    /**
     * Поиск пользователей с указанным статусом активности и последним входом до указанной даты
     */
    List<User> findByIsActiveAndLastLoginBefore(boolean isActive, LocalDateTime lastLoginBefore);
    
    /**
     * Поиск всех активных пользователей
     */
    List<User> findByIsActiveTrue();
    
    /**
     * Поиск пользователей с указанным статусом верификации
     */
    List<User> findByVerified(boolean verified);
    
    // Методы для статистики админ панели
    Long countByVerifiedTrue();
    Long countByCreatedAtAfter(LocalDateTime date);
    Long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Найти пользователей с привязанным Telegram ID
     */
    List<User> findByTelegramIdIsNotNull();

    /**
     * Найти активных пользователей с привязанным Telegram ChatId
     */
    List<User> findByIsActiveTrueAndTelegramChatIdIsNotNull();

    /**
     * Найти пользователей по роли
     */
    List<User> findByRole(String role);

    /**
     * Проверить существование пользователя по Telegram ID
     */
    boolean existsByTelegramId(Long telegramId);

    /**
     * Найти пользователя по коду верификации Discord
     */
    Optional<User> findByDiscordVerificationCode(String discordVerificationCode);
}
