package com.brand.backend.application.auth.handler;

import com.brand.backend.application.auth.command.RefreshTokenCommand;
import com.brand.backend.application.auth.command.RefreshTokenResult;
import com.brand.backend.domain.user.model.RefreshToken;
import com.brand.backend.domain.user.repository.RefreshTokenRepository;
import com.brand.backend.infrastructure.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Обработчик команды обновления токена
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCommandHandler implements CommandHandler<RefreshTokenCommand, RefreshTokenResult> {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Override
    public RefreshTokenResult handle(RefreshTokenCommand command) {
        log.info("Обработка команды обновления токена");

        String refreshToken = command.getRefreshToken();
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            return RefreshTokenResult.builder()
                    .success(false)
                    .message("Refresh token is required")
                    .build();
        }

        Optional<RefreshToken> storedToken = refreshTokenRepository.findByToken(refreshToken);

        if (storedToken.isEmpty() || storedToken.get().getExpiryDate().isBefore(Instant.now())) {
            log.warn("Неверный или просроченный refresh token");
            return RefreshTokenResult.builder()
                    .success(false)
                    .message("Invalid refresh token")
                    .build();
        }

        String username = storedToken.get().getUser().getUsername();
        String newAccessToken = jwtUtil.generateAccessToken(username);

        return RefreshTokenResult.builder()
                .success(true)
                .accessToken(newAccessToken)
                .message("Token refreshed successfully")
                .build();
    }
} 