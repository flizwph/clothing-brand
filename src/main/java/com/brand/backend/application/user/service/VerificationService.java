package com.brand.backend.application.user.service;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Проверяет код верификации
     *
     * @param code код верификации
     * @return пользователь, если код верен, или null
     */
    @Transactional(readOnly = true)
    public User verifyCode(String code) {
        Optional<User> userOptional = userRepository.findByVerificationCode(code);
        if (userOptional.isEmpty()) {
            log.warn("Неверный код верификации: {}", code);
            return null;
        }
        
        return userOptional.get();
    }
    
    /**
     * Генерирует случайный код верификации
     *
     * @return случайный код
     */
    private String generateVerificationCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
} 