package com.brand.backend.application.auth.cqrs.handler.password;

import com.brand.backend.application.auth.cqrs.command.password.ChangePasswordCommand;
import com.brand.backend.application.auth.core.exception.InvalidCredentialsException;
import com.brand.backend.application.auth.core.exception.InvalidPasswordException;
import com.brand.backend.application.auth.core.exception.UserNotFoundException;
import com.brand.backend.application.auth.cqrs.handler.base.CommandHandler;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.RefreshTokenRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.infrastructure.security.audit.SecurityAuditService;
import com.brand.backend.infrastructure.security.audit.SecurityEventType;
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
    private final SecurityAuditService securityAuditService;

    @Override
    @Transactional
    public Boolean handle(ChangePasswordCommand command) {
        log.info("Обработка команды изменения пароля для пользователя: {}", command.getUsername());
        
        // Аудит начала процесса изменения пароля
        securityAuditService.auditInfo(
                SecurityEventType.PASSWORD_CHANGE, 
                command.getUsername(), 
                "Начало процесса изменения пароля"
        );
        
        // Проверка существования пользователя
        Optional<User> userOptional = userRepository.findByUsername(command.getUsername());
        if (userOptional.isEmpty()) {
            log.warn("Пользователь не найден: {}", command.getUsername());
            
            // Аудит ошибки - пользователь не найден
            securityAuditService.auditWarning(
                    SecurityEventType.PASSWORD_CHANGE, 
                    command.getUsername(), 
                    "Попытка изменения пароля для несуществующего пользователя"
            );
            
            throw new UserNotFoundException(command.getUsername());
        }
        
        User user = userOptional.get();
        
        // Проверка текущего пароля
        if (!passwordEncoder.matches(command.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Неверный текущий пароль для пользователя: {}", command.getUsername());
            
            // Аудит ошибки - неверный текущий пароль
            securityAuditService.auditWarning(
                    SecurityEventType.PASSWORD_CHANGE, 
                    command.getUsername(), 
                    "Неверный текущий пароль при попытке изменения пароля"
            );
            
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        
        // Проверка нового пароля на минимальную длину
        if (command.getNewPassword().length() < 6) {
            log.warn("Новый пароль слишком короткий для пользователя: {}", command.getUsername());
            
            // Аудит ошибки - слишком короткий пароль
            securityAuditService.auditInfo(
                    SecurityEventType.PASSWORD_CHANGE, 
                    command.getUsername(), 
                    "Попытка установить слишком короткий пароль"
            );
            
            throw InvalidPasswordException.tooShort(6);
        }
        
        // Проверка нового пароля на совпадение со старым
        if (passwordEncoder.matches(command.getNewPassword(), user.getPasswordHash())) {
            log.warn("Новый пароль совпадает со старым для пользователя: {}", command.getUsername());
            
            // Аудит ошибки - новый пароль совпадает со старым
            securityAuditService.auditInfo(
                    SecurityEventType.PASSWORD_CHANGE, 
                    command.getUsername(), 
                    "Попытка установить пароль, совпадающий с текущим"
            );
            
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
        
        userRepository.save(user);
        log.info("Пароль успешно изменен для пользователя: {}. Все токены инвалидированы.", command.getUsername());
        
        // Аудит успешного изменения пароля
        securityAuditService.auditInfo(
                SecurityEventType.PASSWORD_CHANGE, 
                command.getUsername(), 
                "Пароль успешно изменен. Все токены авторизации инвалидированы."
        );
        
        return true;
    }
} 