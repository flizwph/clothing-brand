package com.brand.backend.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${api.discord.secret-key}")
    private String discordApiSecretKey;
    
    private static final String API_KEY_HEADER = "X-API-KEY";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        
        // Проверяем запросы к Discord API
        if (requestUri.startsWith("/api/discord/verify") || requestUri.startsWith("/api/discord/check-status")) {
            String apiKey = request.getHeader(API_KEY_HEADER);
            
            log.debug("Запрос к Discord API: URI={}, API Key={}, Expected={}", 
                    requestUri, apiKey, discordApiSecretKey);
            
            if (apiKey == null) {
                log.warn("Неавторизованный доступ к Discord API без ключа с IP: {}", request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing API key");
                return;
            }
            
            if (!apiKey.equals(discordApiSecretKey)) {
                log.warn("Неавторизованный доступ к Discord API с неверным ключом с IP: {}", request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
                return;
            }
            
            log.info("Авторизованный доступ к Discord API с IP: {}", request.getRemoteAddr());
        }
        
        filterChain.doFilter(request, response);
    }
} 