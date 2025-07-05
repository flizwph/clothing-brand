package com.brand.backend.application.user.service;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для верификации пользователей через разные источники
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final UserRepository userRepository;

    /**
     * Генерирует и сохраняет код верификации для пользователя
     *
     * @param username имя пользователя
     * @return сгенерированный код верификации
     */
    @Transactional
    public String generateAndSaveVerificationCode(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            log.error("Пользователь не найден: {}", username);
            throw new IllegalArgumentException("Пользователь не найден");
        }

        User user = userOptional.get();
        String code = generateVerificationCode();
        user.setVerificationCode(code);
        userRepository.save(user);
        
        log.info("Сгенерирован код верификации для пользователя {}: {}", username, code);
        return code;
    }
    
    /**
     * Генерирует и сохраняет код верификации для пользователя на основе его Telegram ID
     *
     * @param telegramId ID пользователя в Telegram
     * @return сгенерированный код верификации или null, если пользователь не найден
     */
    @Transactional
    public String generateAndSaveVerificationCodeByTelegramId(Long telegramId) {
        Optional<User> userOptional = userRepository.findByTelegramId(telegramId);
        if (userOptional.isEmpty()) {
            log.error("Пользователь с Telegram ID не найден: {}", telegramId);
            return null;
        }

        User user = userOptional.get();
        String code = generateVerificationCode();
        user.setVerificationCode(code);
        userRepository.save(user);
        
        log.info("Сгенерирован код верификации для пользователя {} с Telegram ID {}: {}", 
                user.getUsername(), telegramId, code);
        return code;
    }
    
    /**
     * Проверяет код верификации и устанавливает статус верифицирован при успешной проверке
     *
     * @param code код верификации
     * @return пользователь, если код верен, или null
     */
    @Transactional
    @CacheEvict(value = "userAuthCache", key = "#result != null ? #result.username : 'unknown'")
    public User verifyCode(String code) {
        Optional<User> userOptional = userRepository.findByVerificationCode(code);
        if (userOptional.isEmpty()) {
            log.warn("Неверный код верификации: {}", code);
            return null;
        }
        
        User user = userOptional.get();
        user.setVerified(true);
        user.setLastUsedVerificationCode(code);
        user.setVerificationCode(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Пользователь {} успешно верифицирован с кодом {}", user.getUsername(), code);
        return user;
    }
    
    /**
     * Генерирует код верификации для Discord
     *
     * @param username имя пользователя
     * @param discordId ID Discord (опционально)
     * @return сгенерированный код верификации
     */
    @Transactional
    public String generateDiscordVerificationCode(String username, String discordId) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            log.error("Пользователь не найден: {}", username);
            throw new IllegalArgumentException("Пользователь не найден");
        }

        User user = userOptional.get();
        String code = generateVerificationCode();
        user.setDiscordVerificationCode(code);
        if (discordId != null && !discordId.isEmpty()) {
            try {
                user.setDiscordId(Long.parseLong(discordId));
            } catch (NumberFormatException e) {
                log.warn("Неверный формат Discord ID: {}", discordId);
            }
        }
        userRepository.save(user);
        
        log.info("Сгенерирован Discord код верификации для пользователя {}: {}", username, code);
        return code;
    }

    /**
     * Генерирует случайный код верификации
     *
     * @return случайный код
     */
    private String generateVerificationCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }
} 