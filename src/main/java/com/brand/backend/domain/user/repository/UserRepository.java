package com.brand.backend.domain.user.repository;

import com.brand.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTelegramId(Long telegramId);
    Optional<User> findByVerificationCode(String verificationCode);
    Optional<User> findByUsername(String username);
    Optional<User> findByDiscordId(Long discordId);
    Optional<User> findByVkUsername(String vkUsername);
}
