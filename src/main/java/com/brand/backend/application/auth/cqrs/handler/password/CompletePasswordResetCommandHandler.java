package com.brand.backend.application.auth.cqrs.handler.password;

import com.brand.backend.application.auth.cqrs.command.password.CompletePasswordResetCommand;
import com.brand.backend.application.auth.core.exception.InvalidPasswordException;
import com.brand.backend.application.auth.core.exception.PasswordResetException;
import com.brand.backend.application.auth.cqrs.handler.base.CommandHandler;
import com.brand.backend.application.auth.infra.repository.PasswordResetTokenRepository;
import com.brand.backend.application.auth.service.notification.TelegramNotificationService;
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
 * Обработчик команды завершения восстановления пароля
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompletePasswordResetCommandHandler implements CommandHandler<CompletePasswordResetCommand, Boolean> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TelegramNotificationService telegramNotificationService;

    @Override
    @Transactional
    public Boolean handle(CompletePasswordResetCommand command) {
        log.info("Обработка команды завершения восстановления пароля для пользователя: {}", command.getUsername());
        
        // Проверка кода восстановления пароля
        Optional<String> usernameOptional = passwordResetTokenRepository.validateToken(command.getResetCode());
        if (usernameOptional.isEmpty() || !usernameOptional.get().equals(command.getUsername())) {
            log.warn("Попытка восстановления пароля с недействительным кодом: {}", command.getResetCode());
            throw PasswordResetException.invalidResetCode();
        }
        
        // Проверка существования пользователя - сначала по email, затем по username
        Optional<User> userOptional = userRepository.findByEmail(command.getUsername());
        if (userOptional.isEmpty()) {
            // Если не найден по email, пробуем найти по username
            userOptional = userRepository.findByUsername(command.getUsername());
        }
        
        if (userOptional.isEmpty()) {
            log.warn("Пользователь не найден: {}", command.getUsername());
            throw PasswordResetException.userNotFound(command.getUsername());
        }
        
        User user = userOptional.get();
        
        // Проверка нового пароля на минимальную длину
        if (command.getNewPassword() == null || command.getNewPassword().length() < 6) {
            log.warn("Новый пароль слишком короткий для пользователя: {}", command.getUsername());
            throw InvalidPasswordException.tooShort(6);
        }
        
        // Проверка нового пароля на совпадение со старым
        if (passwordEncoder.matches(command.getNewPassword(), user.getPasswordHash())) {
            log.warn("Новый пароль совпадает со старым для пользователя: {}", command.getUsername());
            throw InvalidPasswordException.sameAsOld();
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
        
        // Удаление токена восстановления пароля
        passwordResetTokenRepository.removeToken(command.getResetCode());
        
        userRepository.save(user);
        log.info("Пароль успешно изменен для пользователя: {}. Все токены инвалидированы.", command.getUsername());
        
        // Отправка уведомления о смене пароля в Telegram
        telegramNotificationService.sendPasswordChangedNotification(command.getUsername());
        
        return true;
    }
} 