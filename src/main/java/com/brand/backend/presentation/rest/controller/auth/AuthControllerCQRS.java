package com.brand.backend.presentation.rest.controller.auth;

import com.brand.backend.application.auth.command.LoginCommandResult;
import com.brand.backend.application.auth.command.RefreshTokenResult;
import com.brand.backend.application.auth.exception.UserBlockedException;
import com.brand.backend.application.auth.service.AuthServiceCQRS;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.UserLoginRequest;
import com.brand.backend.presentation.dto.request.UserRegistrationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для аутентификации с использованием CQRS
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/v2")
@RequiredArgsConstructor
public class AuthControllerCQRS {

    private final AuthServiceCQRS authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody @Valid UserRegistrationRequest request) {
        log.info("Получен запрос на регистрацию: {}", request.getUsername());
        Map<String, String> response = new HashMap<>();

        try {
            User user = authService.registerUser(request.getUsername(), request.getPassword());

            response.put("message", "User registered successfully");
            response.put("verificationCode", user.getVerificationCode());

            log.info("Регистрация успешна для пользователя: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if ("Username already exists".equals(e.getMessage())) {
                log.warn("Попытка регистрации уже существующего логина: {}", request.getUsername());
                response.put("error", "Username already exists");
                response.put("message", "Этот логин уже занят.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            log.error("Ошибка во время регистрации: {}", e.getMessage(), e);
            response.put("error", "Internal Server Error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody @Valid UserLoginRequest request) {
        try {
            log.info("📥 [LOGIN] Получен запрос на вход: {}", request.getUsername());

            LoginCommandResult result = authService.login(request.getUsername(), request.getPassword());
            Map<String, String> response = new HashMap<>();

            if (!result.isSuccess()) {
                log.info("❌ [LOGIN] Неудачная попытка входа: {}", request.getUsername());
                response.put("message", result.getMessage());
                
                if (result.isNeedsVerification()) {
                    response.put("verificationCode", result.getVerificationCode());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            log.info("✅ [LOGIN SUCCESS] Успешный вход: {}", request.getUsername());
            response.put("message", result.getMessage());
            response.put("accessToken", result.getAccessToken());
            response.put("refreshToken", result.getRefreshToken());

            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + result.getAccessToken())
                    .body(response);
        } catch (UserBlockedException e) {
            log.warn("🔒 [LOGIN BLOCKED] Заблокированный пользователь пытается войти: {}", request.getUsername());
            
            Map<String, String> response = new HashMap<>();
            response.put("error", "User blocked");
            response.put("message", e.getMessage());
            response.put("minutesLeft", String.valueOf(e.getMinutesLeft()));
            
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        } catch (Exception e) {
            log.error("🔥 [LOGIN ERROR] Ошибка входа для пользователя {}: {}", 
                    request.getUsername(), e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred during login");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshAccessToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || refreshToken.isBlank()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Refresh token is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            RefreshTokenResult result = authService.refreshToken(refreshToken);
            Map<String, String> response = new HashMap<>();
            
            if (!result.isSuccess()) {
                response.put("message", result.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            response.put("accessToken", result.getAccessToken());
            response.put("message", result.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("🔥 [REFRESH ERROR] Unexpected error: {}", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred during token refresh");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            
            if (username == null || username.isBlank()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Username is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            boolean result = authService.logout(username);
            
            if (!result) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("🔥 [LOGOUT ERROR] Ошибка при выходе: {}", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred during logout");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 