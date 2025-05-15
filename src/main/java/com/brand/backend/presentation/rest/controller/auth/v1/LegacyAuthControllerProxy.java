package com.brand.backend.presentation.rest.controller.auth.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Прокси-контроллер для перенаправления запросов с v1 API на v2 API
 * Заменяет оригинальный AuthController из v1.
 * 
 * @deprecated Этот API устарел. Используйте /api/auth/v2 вместо него.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Deprecated(forRemoval = true)
public class LegacyAuthControllerProxy {

    @Value("${api.legacy.deprecation-message:This API endpoint is deprecated. Please use /api/auth/v2 endpoints instead.}")
    private String deprecationMessage;
    
    @Value("${api.legacy.redirect-enabled:false}")
    private boolean redirectEnabled;

    /**
     * Обрабатывает запрос регистрации и перенаправляет на новый API
     *
     * @deprecated Используйте /api/auth/v2/register вместо этого
     */
    @PostMapping("/register")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Map<String, String>> registerUser(HttpServletRequest request, HttpServletResponse response, @RequestBody String body) {
        log.warn("Обращение к устаревшему эндпоинту: /api/auth/register");
        
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", deprecationMessage);
        responseMap.put("newEndpoint", "/api/auth/v2/register");
        
        response.setHeader("X-Deprecated-API", "true");
        response.setHeader("X-New-API-Endpoint", "/api/auth/v2/register");
        
        if (redirectEnabled) {
            try {
                // В реальной реализации здесь был бы внутренний вызов к новому API
                log.info("Выполняется перенаправление на новый API");
                responseMap.put("redirected", "true");
                return ResponseEntity.status(HttpStatus.OK).body(responseMap);
            } catch (Exception e) {
                log.error("Ошибка при перенаправлении на новый API: {}", e.getMessage());
                responseMap.put("error", "Failed to process with new API");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMap);
            }
        }
        
        return ResponseEntity.status(HttpStatus.GONE).body(responseMap);
    }
    
    /**
     * Обрабатывает запрос входа в систему и перенаправляет на новый API
     *
     * @deprecated Используйте /api/auth/v2/login вместо этого
     */
    @PostMapping("/login")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Map<String, String>> loginUser(HttpServletRequest request, HttpServletResponse response, @RequestBody String body) {
        log.warn("Обращение к устаревшему эндпоинту: /api/auth/login");
        
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", deprecationMessage);
        responseMap.put("newEndpoint", "/api/auth/v2/login");
        
        response.setHeader("X-Deprecated-API", "true");
        response.setHeader("X-New-API-Endpoint", "/api/auth/v2/login");
        
        if (redirectEnabled) {
            try {
                // В реальной реализации здесь был бы внутренний вызов к новому API
                log.info("Выполняется перенаправление на новый API");
                responseMap.put("redirected", "true");
                return ResponseEntity.status(HttpStatus.OK).body(responseMap);
            } catch (Exception e) {
                log.error("Ошибка при перенаправлении на новый API: {}", e.getMessage());
                responseMap.put("error", "Failed to process with new API");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMap);
            }
        }
        
        return ResponseEntity.status(HttpStatus.GONE).body(responseMap);
    }
    
    /**
     * Обрабатывает запрос обновления токена и перенаправляет на новый API
     *
     * @deprecated Используйте /api/auth/v2/refresh вместо этого
     */
    @PostMapping("/refresh")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Map<String, String>> refreshAccessToken(HttpServletRequest request, HttpServletResponse response, @RequestBody String body) {
        log.warn("Обращение к устаревшему эндпоинту: /api/auth/refresh");
        
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", deprecationMessage);
        responseMap.put("newEndpoint", "/api/auth/v2/refresh");
        
        response.setHeader("X-Deprecated-API", "true");
        response.setHeader("X-New-API-Endpoint", "/api/auth/v2/refresh");
        
        if (redirectEnabled) {
            try {
                // В реальной реализации здесь был бы внутренний вызов к новому API
                log.info("Выполняется перенаправление на новый API");
                responseMap.put("redirected", "true");
                return ResponseEntity.status(HttpStatus.OK).body(responseMap);
            } catch (Exception e) {
                log.error("Ошибка при перенаправлении на новый API: {}", e.getMessage());
                responseMap.put("error", "Failed to process with new API");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMap);
            }
        }
        
        return ResponseEntity.status(HttpStatus.GONE).body(responseMap);
    }
    
    /**
     * Обрабатывает запрос выхода из системы и перенаправляет на новый API
     *
     * @deprecated Используйте /api/auth/v2/logout вместо этого
     */
    @PostMapping("/logout")
    @Deprecated(forRemoval = true)
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response,
                                   @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        log.warn("Обращение к устаревшему эндпоинту: /api/auth/logout");
        
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", deprecationMessage);
        responseMap.put("newEndpoint", "/api/auth/v2/logout");
        
        response.setHeader("X-Deprecated-API", "true");
        response.setHeader("X-New-API-Endpoint", "/api/auth/v2/logout");
        
        if (redirectEnabled) {
            try {
                // В реальной реализации здесь был бы внутренний вызов к новому API
                log.info("Выполняется перенаправление на новый API");
                responseMap.put("redirected", "true");
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