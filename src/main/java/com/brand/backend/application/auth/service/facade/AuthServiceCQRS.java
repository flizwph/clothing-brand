package com.brand.backend.application.auth.service.facade;

import com.brand.backend.application.auth.bus.CommandBus;
import com.brand.backend.application.auth.bus.QueryBus;
import com.brand.backend.application.auth.cqrs.command.password.ChangePasswordCommand;
import com.brand.backend.application.auth.cqrs.command.password.CompletePasswordResetCommand;
import com.brand.backend.application.auth.cqrs.command.password.InitiatePasswordResetCommand;
import com.brand.backend.application.auth.cqrs.command.user.LoginCommand;
import com.brand.backend.application.auth.cqrs.result.LoginCommandResult;
import com.brand.backend.application.auth.cqrs.command.user.LogoutCommand;
import com.brand.backend.application.auth.cqrs.command.user.RefreshTokenCommand;
import com.brand.backend.application.auth.cqrs.result.RefreshTokenResult;
import com.brand.backend.application.auth.cqrs.command.user.RegisterUserCommand;
import com.brand.backend.application.auth.cqrs.query.user.ValidateTokenQuery;
import com.brand.backend.application.auth.cqrs.result.ValidateTokenResult;
import com.brand.backend.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.Optional;

/**
 * Сервис аутентификации, реализованный с использованием CQRS и шин команд/запросов
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceCQRS {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /**
     * Регистрирует нового пользователя
     */
    @Transactional
    public User registerUser(String username, String email, String password, String confirmPassword) {
        log.info("Регистрация нового пользователя: {} с email: {}", username, email);
        RegisterUserCommand command = RegisterUserCommand.builder()
                .username(username)
                .email(email)
                .password(password)
                .confirmPassword(confirmPassword)
                .build();
                
        return commandBus.dispatch(command);
    }

    /**
     * Аутентифицирует пользователя
     */
    public Optional<User> authenticateUser(String username, String password) {
        log.info("Аутентификация пользователя: {}", username);
        LoginCommand command = LoginCommand.builder()
                .username(username)
                .password(password)
                .build();
                
        LoginCommandResult result = commandBus.dispatch(command);
        
        if (result.isSuccess()) {
            return Optional.of(result.getUser());
        }
        
        return Optional.empty();
    }

    /**
     * Полная логика входа с получением токенов
     */
    public LoginCommandResult login(String username, String password) {
        log.info("Вход пользователя: {}", username);
        LoginCommand command = LoginCommand.builder()
                .username(username)
                .password(password)
                .build();
                
        return commandBus.dispatch(command);
    }

    /**
     * Обновляет токен доступа
     */
    public RefreshTokenResult refreshToken(String refreshToken) {
        log.info("Обновление токена доступа");
        RefreshTokenCommand command = RefreshTokenCommand.builder()
                .refreshToken(refreshToken)
                .build();
                
        return commandBus.dispatch(command);
    }

    /**
     * Выполняет выход пользователя
     */
    @Transactional
    public boolean logout(String username) {
        log.info("Выход пользователя: {}", username);
        LogoutCommand command = LogoutCommand.builder()
                .username(username)
                .build();
                
        return commandBus.dispatch(command);
    }
    
    /**
     * Валидирует токен
     */
    public ValidateTokenResult validateToken(String token) {
        log.info("Валидация токена");
        ValidateTokenQuery query = ValidateTokenQuery.builder()
                .token(token)
                .build();
                
        return queryBus.dispatch(query);
    }
    
    /**
     * Изменяет пароль пользователя и инвалидирует все его токены
     */
    @Transactional
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        log.info("Изменение пароля для пользователя: {}", username);
        ChangePasswordCommand command = ChangePasswordCommand.builder()
                .username(username)
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .build();
                
        return commandBus.dispatch(command);
    }
    
    /**
     * Инициирует процесс восстановления пароля
     */
    public String initiatePasswordReset(String username) {
        log.info("Инициация восстановления пароля для пользователя: {}", username);
        InitiatePasswordResetCommand command = InitiatePasswordResetCommand.builder()
                .username(username)
                .build();
                
        return commandBus.dispatch(command);
    }
    
    /**
     * Завершает процесс восстановления пароля
     */
    @Transactional
    public boolean completePasswordReset(String username, String resetCode, String newPassword) {
        log.info("Завершение восстановления пароля для пользователя: {}", username);
        CompletePasswordResetCommand command = CompletePasswordResetCommand.builder()
                .username(username)
                .resetCode(resetCode)
                .newPassword(newPassword)
                .build();
                
        return commandBus.dispatch(command);
    }
} 