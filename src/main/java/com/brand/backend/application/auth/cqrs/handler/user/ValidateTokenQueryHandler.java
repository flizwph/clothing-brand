package com.brand.backend.application.auth.cqrs.handler.user;

import com.brand.backend.application.auth.cqrs.handler.base.QueryHandler;
import com.brand.backend.application.auth.cqrs.query.user.ValidateTokenQuery;
import com.brand.backend.application.auth.cqrs.result.ValidateTokenResult;
import com.brand.backend.infrastructure.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;

/**
 * Обработчик запроса для валидации токена
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateTokenQueryHandler implements QueryHandler<ValidateTokenQuery, ValidateTokenResult> {

    private final JwtUtil jwtUtil;

    @Override
    public ValidateTokenResult handle(ValidateTokenQuery query) {
        log.info("Обработка запроса на валидацию токена");
        
        try {
            String username = jwtUtil.extractUsername(query.getToken());
            
            return ValidateTokenResult.builder()
                    .valid(true)
                    .username(username)
                    .message("Token is valid")
                    .build();
        } catch (ExpiredJwtException e) {
            log.warn("Токен просрочен: {}", e.getMessage());
            return ValidateTokenResult.builder()
                    .valid(false)
                    .message("Token has expired")
                    .build();
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            log.warn("Неверный формат токена: {}", e.getMessage());
            return ValidateTokenResult.builder()
                    .valid(false)
                    .message("Invalid token format")
                    .build();
        } catch (Exception e) {
            log.warn("Ошибка при валидации токена: {}", e.getMessage());
            return ValidateTokenResult.builder()
                    .valid(false)
                    .message("Token validation error")
                    .build();
        }
    }
} 