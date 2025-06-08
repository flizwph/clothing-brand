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

@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutCommandHandler implements CommandHandler<LogoutCommand, Boolean> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Boolean handle(LogoutCommand command) {
        log.info("Processing logout command for user: {}", command.getUsername());

        Optional<User> userOptional = userRepository.findByUsername(command.getUsername());
        
        if (userOptional.isEmpty()) {
            log.warn("User not found: {}", command.getUsername());
            return false;
        }
        
        User user = userOptional.get();
        refreshTokenRepository.deleteByUser(user);
        log.info("Logout completed for user: {}", command.getUsername());
        
        return true;
    }
} 