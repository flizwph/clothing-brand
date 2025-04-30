package com.brand.backend.application.auth.service;

import com.brand.backend.application.auth.bus.CommandBus;
import com.brand.backend.application.auth.bus.QueryBus;
import com.brand.backend.application.auth.command.LoginCommand;
import com.brand.backend.application.auth.command.LoginCommandResult;
import com.brand.backend.application.auth.command.LogoutCommand;
import com.brand.backend.application.auth.command.RefreshTokenCommand;
import com.brand.backend.application.auth.command.RefreshTokenResult;
import com.brand.backend.application.auth.command.RegisterUserCommand;
import com.brand.backend.application.auth.query.ValidateTokenQuery;
import com.brand.backend.application.auth.query.ValidateTokenResult;
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
    public User registerUser(String username, String password) {
        log.info("Регистрация нового пользователя: {}", username);
        RegisterUserCommand command = RegisterUserCommand.builder()
                .username(username)
                .password(password)
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
} 