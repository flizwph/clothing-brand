package com.brand.backend.application.auth.handler;

import com.brand.backend.application.auth.command.LoginCommand;
import com.brand.backend.application.auth.command.LoginCommandResult;
import com.brand.backend.application.auth.exception.UserBlockedException;
import com.brand.backend.application.auth.service.LoginAttemptService;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.infrastructure.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    @Override
    public LoginCommandResult handle(LoginCommand command) {
        log.info("Попытка входа в систему: {}", command.getUsername());
        
        // Проверка на блокировку
        if (loginAttemptService.isBlocked(command.getUsername())) {
            log.warn("Пользователь {} заблокирован из-за слишком большого количества неудачных попыток", 
                    command.getUsername());
            throw new UserBlockedException(command.getUsername(), 30); // 30 минут (по умолчанию)
        }

        Optional<User> userOptional = userRepository.findUserForAuth(command.getUsername());

        if (userOptional.isEmpty()) {
            log.warn("Пользователь не найден: {}", command.getUsername());
            loginAttemptService.loginFailed(command.getUsername());
            return LoginCommandResult.builder()
                    .success(false)
                    .message("Invalid credentials")
                    .build();
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            log.warn("Пароль НЕ совпадает для пользователя: {}", command.getUsername());
            loginAttemptService.loginFailed(command.getUsername());
            
            int attemptsLeft = loginAttemptService.getRemainingAttempts(command.getUsername());
            String message = attemptsLeft > 0 
                    ? String.format("Invalid credentials. %d attempts left", attemptsLeft)
                    : "Invalid credentials. Your account is now blocked.";
                    
            return LoginCommandResult.builder()
                    .success(false)
                    .message(message)
                    .build();
        }

        if (!user.isVerified()) {
            log.warn("Аккаунт {} не верифицирован!", command.getUsername());
            // Не считаем попытку входа неверифицированного пользователя как неудачную
            return LoginCommandResult.builder()
                    .success(false)
                    .needsVerification(true)
                    .verificationCode(user.getVerificationCode())
                    .message("Account not verified")
                    .build();
        }

        // Успешный вход
        loginAttemptService.loginSucceeded(command.getUsername());
        
        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        String refreshToken = generateRefreshToken(user);

        // Асинхронное обновление lastLogin можно добавить позже

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
        return UUID.randomUUID().toString();
    }
} 