package com.brand.backend.application.auth.cqrs.handler.user;

import com.brand.backend.application.auth.cqrs.command.user.LoginCommand;
import com.brand.backend.application.auth.cqrs.result.LoginCommandResult;
import com.brand.backend.application.auth.core.exception.InvalidCredentialsException;
import com.brand.backend.application.auth.core.exception.UserBlockedException;
import com.brand.backend.application.auth.core.exception.UserNotVerifiedException;
import com.brand.backend.application.auth.core.exception.AccountDisabledException;
import com.brand.backend.application.auth.cqrs.handler.base.CommandHandler;
import com.brand.backend.application.auth.service.security.LoginAttemptService;
import com.brand.backend.application.auth.service.facade.AccountManagementService;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.model.RefreshToken;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.domain.user.repository.RefreshTokenRepository;
import com.brand.backend.infrastructure.security.audit.SecurityAuditService;
import com.brand.backend.infrastructure.security.audit.SecurityEventType;
import com.brand.backend.infrastructure.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Обработчик команды входа в систему
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginCommandHandler implements CommandHandler<LoginCommand, LoginCommandResult> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditService securityAuditService;
    private final AccountManagementService accountManagementService;

    @Override
    public LoginCommandResult handle(LoginCommand command) {
        log.info("Попытка входа в систему: {}", command.getUsername());
        
        // Аудит начала процесса входа
        securityAuditService.auditInfo(
                SecurityEventType.LOGIN_FAILURE, 
                command.getUsername(), 
                "Попытка входа"
        );
        
        // Проверка на блокировку
        if (loginAttemptService.isBlocked(command.getUsername())) {
            log.warn("Пользователь {} заблокирован из-за слишком большого количества неудачных попыток", 
                    command.getUsername());
                    
            // Аудит блокировки
            securityAuditService.auditWarning(
                    SecurityEventType.ACCOUNT_LOCKED, 
                    command.getUsername(), 
                    "Учетная запись временно заблокирована из-за превышения лимита попыток входа"
            );
            
            throw new UserBlockedException(command.getUsername(), 30); // 30 минут (по умолчанию)
        }

        Optional<User> userOptional = userRepository.findByUsername(command.getUsername());

        if (userOptional.isEmpty()) {
            log.warn("Пользователь не найден: {}", command.getUsername());
            loginAttemptService.loginFailed(command.getUsername());
            
            // Аудит неудачного входа - пользователь не найден
            securityAuditService.auditWarning(
                    SecurityEventType.LOGIN_FAILURE, 
                    command.getUsername(), 
                    "Пользователь не найден"
            );
            
            throw new InvalidCredentialsException("Invalid username or password");
        }

        User user = userOptional.get();
        
        // Проверка активности аккаунта
        if (!user.isActive()) {
            log.warn("Попытка входа в деактивированный аккаунт: {}", command.getUsername());
            
            // Аудит попытки входа в неактивный аккаунт
            securityAuditService.auditWarning(
                    SecurityEventType.LOGIN_FAILURE, 
                    command.getUsername(), 
                    "Попытка входа в деактивированный аккаунт"
            );
            
            throw new AccountDisabledException(command.getUsername());
        }

        if (!passwordEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            log.warn("Пароль НЕ совпадает для пользователя: {}", command.getUsername());
            loginAttemptService.loginFailed(command.getUsername());
            
            int attemptsLeft = loginAttemptService.getRemainingAttempts(command.getUsername());
            
            // Аудит неудачного входа - неверный пароль
            securityAuditService.auditWarning(
                    SecurityEventType.LOGIN_FAILURE, 
                    command.getUsername(), 
                    "Неверный пароль. Осталось попыток: " + attemptsLeft
            );
            
            // Если это последняя попытка перед блокировкой, записываем критическое событие
            if (attemptsLeft <= 1) {
                securityAuditService.auditCritical(
                        SecurityEventType.BRUTE_FORCE_ATTEMPT, 
                        command.getUsername(), 
                        "Возможная brute-force атака. Учетная запись будет заблокирована после следующей попытки."
                );
            }
            
            String message = attemptsLeft > 0 
                    ? String.format("Invalid username or password. %d attempts left", attemptsLeft)
                    : "Invalid username or password. Your account will be blocked after this attempt.";
                    
            throw new InvalidCredentialsException(message);
        }

        if (!user.isVerified()) {
            log.warn("Аккаунт {} не верифицирован!", command.getUsername());
            
            // Аудит - попытка входа в неверифицированный аккаунт
            securityAuditService.auditInfo(
                    SecurityEventType.LOGIN_FAILURE, 
                    command.getUsername(), 
                    "Попытка входа в неверифицированный аккаунт"
            );
            
            // Не считаем попытку входа неверифицированного пользователя как неудачную
            throw new UserNotVerifiedException(user.getUsername(), user.getVerificationCode());
        }

        // Успешный вход
        loginAttemptService.loginSucceeded(command.getUsername());
        
        // Передаем версию токена при генерации
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getTokenVersion());
        String refreshToken = generateRefreshToken(user);
        
        // Обновляем время последнего входа
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        // Аудит успешного входа
        securityAuditService.auditInfo(
                SecurityEventType.LOGIN_SUCCESS, 
                command.getUsername(), 
                "Успешный вход в систему"
        );

        return LoginCommandResult.builder()
                .success(true)
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("Login successful")
                .build();
    }

    // Временно дублируем логику из AuthService
    private String generateRefreshToken(User user) {
        // Обновляем или создаем запись в базе для refresh токена
        String token = UUID.randomUUID().toString();
        
        // Сохраняем токен в базе данных
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);
        
        // Создаем дату истечения срока (7 дней)
        Instant expiryDate = Instant.now().plusMillis(604800000);
        
        if (existingToken.isPresent()) {
            // Обновляем существующий токен
            RefreshToken refreshToken = existingToken.get();
            refreshToken.setToken(token);
            refreshToken.setExpiryDate(expiryDate);
            refreshTokenRepository.save(refreshToken);
            log.debug("Обновлен существующий refresh token для пользователя: {}", user.getUsername());
        } else {
            // Создаем новый токен
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUser(user);
            refreshToken.setToken(token);
            refreshToken.setExpiryDate(expiryDate);
            refreshTokenRepository.save(refreshToken);
            log.debug("Создан новый refresh token для пользователя: {}", user.getUsername());
        }
        
        return token;
    }
} 