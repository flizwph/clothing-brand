package com.brand.backend.application.auth.handler;

import com.brand.backend.application.auth.command.ChangePasswordCommand;
import com.brand.backend.application.auth.exception.InvalidCredentialsException;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.RefreshTokenRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Обработчик команды изменения пароля с инвалидацией всех токенов
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChangePasswordCommandHandler implements CommandHandler<ChangePasswordCommand, Boolean> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Boolean handle(ChangePasswordCommand command) {
        log.info("Обработка команды изменения пароля для пользователя: {}", command.getUsername());
        
        // Проверка существования пользователя
        Optional<User> userOptional = userRepository.findByUsername(command.getUsername());
        if (userOptional.isEmpty()) {
            log.warn("Пользователь не найден: {}", command.getUsername());
            throw new InvalidCredentialsException("User not found");
        }
        
        User user = userOptional.get();
        
        // Проверка текущего пароля
        if (!passwordEncoder.matches(command.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Неверный текущий пароль для пользователя: {}", command.getUsername());
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        
        // Проверка нового пароля на совпадение со старым
        if (passwordEncoder.matches(command.getNewPassword(), user.getPasswordHash())) {
            log.warn("Новый пароль совпадает со старым для пользователя: {}", command.getUsername());
            throw new InvalidCredentialsException("New password must be different from the current one");
        }
        
        // Изменение пароля
        user.setPasswordHash(passwordEncoder.encode(command.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        
        // Инвалидация всех токенов путем увеличения версии токена
        if (user.getTokenVersion() == null) {
            user.setTokenVersion(1);
        } else {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        
        // Удаление всех refresh-токенов пользователя
        refreshTokenRepository.deleteByUser(user);
        
        userRepository.save(user);
        log.info("Пароль успешно изменен для пользователя: {}. Все токены инвалидированы.", command.getUsername());
        
        return true;
    }
} 