package com.brand.backend.application.auth.service;

import java.security.SecureRandom;
import com.brand.backend.domain.user.model.RefreshToken;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.RefreshTokenRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(User user, String rawPassword) {
        log.info("Попытка регистрации нового пользователя: {}", user.getUsername());

        if (rawPassword == null || rawPassword.isEmpty()) {
            log.error("Пароль не может быть пустым!");
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            log.warn("Пользователь с таким именем уже существует: {}", user.getUsername());
            throw new RuntimeException("Username already exists");
        }

        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerified(false);

        log.info("Пользователь зарегистрирован: {}", user.getUsername());
        return userRepository.save(user);
    }

    public Optional<User> authenticateUser(String username, String password) {
        log.info("Попытка входа в систему: {}", username);

        Optional<User> userOptional = userRepository.findUserForAuth(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            log.info("Пользователь найден в базе: {}", username);
            log.debug("Хешированный пароль в базе: {}", user.getPasswordHash());
            log.debug("Введенный пароль: {}", password);

            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                log.info("Пароль совпадает для пользователя: {}", username);
                return userOptional;
            } else {
                log.warn("Пароль НЕ совпадает для пользователя: {}", username);
            }
        } else {
            log.warn("Пользователь не найден: {}", username);
        }
        return Optional.empty();
    }

    // ✅ Генерация Refresh Token
    @Transactional
    public String generateRefreshToken(User user) {
        try {
            String token = UUID.randomUUID().toString();
            Instant expiryDate = Instant.now().plusMillis(604800000); // 7 дней
            
            // Пробуем обновить существующий токен
            int updated = refreshTokenRepository.updateTokenForUser(token, expiryDate, user.getId());
            
            // Если не обновили (не существует), создаем новый
            if (updated == 0) {
                try {
                    refreshTokenRepository.insertToken(user.getId(), token, expiryDate);
                    log.debug("Создан новый refresh token для пользователя ID={}", user.getId());
                } catch (Exception e) {
                    log.warn("Ошибка при создании refresh-токена: {}", e.getMessage());
                }
            } else {
                log.debug("Обновлен существующий refresh token для пользователя ID={}", user.getId());
            }
            
            return token;
        } catch (Exception e) {
            log.error("Критическая ошибка при генерации refresh token для пользователя ID={}: {}", 
                    user.getId(), e.getMessage(), e);
            // В случае ошибки создаем токен без сохранения в БД
            return UUID.randomUUID().toString();
        }
    }

    // ✅ Обновление Access Token по Refresh Token
    public String refreshAccessToken(String refreshToken) {
        Optional<RefreshToken> storedToken = refreshTokenRepository.findByToken(refreshToken);

        if (storedToken.isEmpty() || storedToken.get().getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Invalid refresh token");
        }

        return storedToken.get().getUser().getUsername();
    }

    // ✅ Logout (удаляем Refresh Token)
    @Transactional
    public void logout(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    private String generateVerificationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 32; i++) {
            int index = random.nextInt(chars.length());
            code.append(chars.charAt(index));
        }

        return code.toString();
    }

}
