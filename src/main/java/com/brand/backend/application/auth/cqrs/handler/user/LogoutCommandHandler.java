package com.brand.backend.application.auth.cqrs.handler.user;

import com.brand.backend.application.auth.cqrs.command.user.LogoutCommand;
import com.brand.backend.application.auth.cqrs.handler.base.CommandHandler;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.RefreshTokenRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Обработчик команды выхода из системы
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutCommandHandler implements CommandHandler<LogoutCommand, Boolean> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Boolean handle(LogoutCommand command) {
        log.info("Обработка команды выхода из системы для пользователя: {}", command.getUsername());

        Optional<User> userOptional = userRepository.findByUsername(command.getUsername());
        
        if (userOptional.isEmpty()) {
            log.warn("Пользователь не найден: {}", command.getUsername());
            return false;
        }
        
        User user = userOptional.get();
        refreshTokenRepository.deleteByUser(user);
        log.info("Выход из системы выполнен для пользователя: {}", command.getUsername());
        
        return true;
    }
} 