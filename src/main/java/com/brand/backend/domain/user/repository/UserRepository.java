package com.brand.backend.domain.user.repository;

import com.brand.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.Cacheable;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTelegramId(Long telegramId);
    Optional<User> findByVerificationCode(String verificationCode);
    Optional<User> findByUsername(String username);
    Optional<User> findByDiscordId(Long discordId);
    Optional<User> findByVkUsername(String vkUsername);
    
    // Оптимизированный запрос для аутентификации с кэшированием
    @Cacheable(value = "userAuthCache", key = "#username")
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findUserForAuth(@Param("username") String username);
}
