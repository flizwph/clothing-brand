package com.brand.backend.presentation.rest.controller.auth.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Прокси-контроллер для перенаправления запросов валидации токенов с v1 API на v2 API
 * Заменяет оригинальный TokenValidationController из v1.
 * 
 * @deprecated Этот API устарел. Используйте /api/auth/v2/validate-token вместо него.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/token")
@RequiredArgsConstructor
@Deprecated(forRemoval = true)
public class LegacyTokenValidationProxy {

    @Value("${api.legacy.deprecation-message:This API endpoint is deprecated. Please use /api/auth/v2 endpoints instead.}")
    private String deprecationMessage;
    
    @Value("${api.legacy.redirect-enabled:false}")
    private boolean redirectEnabled;

    /**
     * Проверяет валидность JWT-токена и перенаправляет на новый API
     *
     * @deprecated Используйте /api/auth/v2/validate-token вместо этого
     */
    @PostMapping("/validate")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Map<String, Object>> validateToken(
            HttpServletRequest request, 
            HttpServletResponse response, 
            @RequestBody Map<String, String> requestBody) {
        
        log.warn("Обращение к устаревшему эндпоинту: /api/auth/token/validate");
        
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("valid", false);
        responseMap.put("message", deprecationMessage);
        responseMap.put("newEndpoint", "/api/auth/v2/validate-token");
        
        response.setHeader("X-Deprecated-API", "true");
        response.setHeader("X-New-API-Endpoint", "/api/auth/v2/validate-token");
        response.setHeader("X-API-Method-Change", "POST → GET");
        
        if (redirectEnabled) {
            try {
                // В реальной реализации здесь был бы внутренний вызов к новому API
                log.info("Выполняется перенаправление на новый API");
                responseMap.put("redirected", "true");
                responseMap.put("note", "Method changed from POST to GET. Token should be in Authorization header instead of request body.");
                return ResponseEntity.status(HttpStatus.OK).body(responseMap);
            } catch (Exception e) {
                log.error("Ошибка при перенаправлении на новый API: {}", e.getMessage());
                responseMap.put("error", "Failed to process with new API");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMap);
            }
        }
        
        return ResponseEntity.status(HttpStatus.GONE).body(responseMap);
    }
} 