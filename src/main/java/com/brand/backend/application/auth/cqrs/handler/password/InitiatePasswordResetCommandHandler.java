package com.brand.backend.application.auth.cqrs.handler.password;

import com.brand.backend.application.auth.cqrs.command.password.InitiatePasswordResetCommand;
import com.brand.backend.application.auth.core.exception.PasswordResetException;
import com.brand.backend.application.auth.cqrs.handler.base.CommandHandler;
import com.brand.backend.application.auth.infra.repository.PasswordResetTokenRepository;
import com.brand.backend.application.auth.service.notification.TelegramNotificationService;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Обработчик команды инициации восстановления пароля
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitiatePasswordResetCommandHandler implements CommandHandler<InitiatePasswordResetCommand, String> {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TelegramNotificationService telegramNotificationService;
    
    @Value("${security.password-reset.token-expiration-minutes:30}")
    private int tokenExpirationMinutes;
    
    @Override
    public String handle(InitiatePasswordResetCommand command) {
        log.info("Обработка команды инициации восстановления пароля для пользователя: {}", command.getUsername());
        
        // Проверка существования пользователя
        Optional<User> userOptional = userRepository.findByUsername(command.getUsername());
        if (userOptional.isEmpty()) {
            log.warn("Попытка восстановления пароля для несуществующего пользователя: {}", command.getUsername());
            throw PasswordResetException.userNotFound(command.getUsername());
        }
        
        User user = userOptional.get();
        
        // Проверка, что пользователь верифицирован
        if (!user.isVerified()) {
            log.warn("Попытка восстановления пароля для неверифицированного пользователя: {}", command.getUsername());
            throw PasswordResetException.userNotVerified(command.getUsername());
        }
        
        // Проверка, что у пользователя есть привязка к Telegram
        if (user.getTelegramChatId() == null || user.getTelegramChatId().isEmpty()) {
            log.warn("Попытка восстановления пароля для пользователя без привязки к Telegram: {}", command.getUsername());
            throw PasswordResetException.noTelegramAccount(command.getUsername());
        }
        
        // Проверка, что у пользователя нет активного запроса на восстановление пароля
        if (passwordResetTokenRepository.hasActiveToken(command.getUsername())) {
            log.warn("Попытка восстановления пароля при наличии активного запроса: {}", command.getUsername());
            throw PasswordResetException.activeRequestExists(command.getUsername());
        }
        
        // Создание токена восстановления пароля
        String resetToken = passwordResetTokenRepository.createToken(command.getUsername(), tokenExpirationMinutes);
        
        // Отправка кода восстановления пароля в Telegram
        boolean sentToTelegram = telegramNotificationService.sendPasswordResetCode(command.getUsername(), resetToken);
        
        if (!sentToTelegram) {
            log.error("Не удалось отправить код восстановления пароля в Telegram: {}", command.getUsername());
            // Удаляем созданный токен, чтобы пользователь мог повторить попытку
            passwordResetTokenRepository.removeToken(resetToken);
            throw new PasswordResetException("Failed to send password reset code to Telegram");
        }
        
        log.info("Код восстановления пароля успешно отправлен в Telegram для пользователя: {}", command.getUsername());
        
        // Возвращаем замаскированный токен для проверки (только для тестирования)
        return "Reset code has been sent to your Telegram account. Please check your messages.";
    }
} 