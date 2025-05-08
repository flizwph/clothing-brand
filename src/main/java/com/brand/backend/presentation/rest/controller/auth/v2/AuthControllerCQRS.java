package com.brand.backend.presentation.rest.controller.auth.v2;

import com.brand.backend.application.auth.cqrs.result.LoginCommandResult;
import com.brand.backend.application.auth.cqrs.result.RefreshTokenResult;
import com.brand.backend.application.auth.core.exception.InvalidCredentialsException;
import com.brand.backend.application.auth.core.exception.UserBlockedException;
import com.brand.backend.application.auth.core.exception.UserNotVerifiedException;
import com.brand.backend.application.auth.core.exception.UsernameExistsException;
import com.brand.backend.application.auth.cqrs.result.ValidateTokenResult;
import com.brand.backend.application.auth.service.facade.AuthServiceCQRS;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.ChangePasswordRequest;
import com.brand.backend.presentation.dto.request.UserLoginRequest;
import com.brand.backend.presentation.dto.request.UserRegistrationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
        } catch (UsernameExistsException e) {
            log.warn("Попытка регистрации уже существующего логина: {}", request.getUsername());
            response.put("error", "Username already exists");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (RuntimeException e) {
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
        } catch (UserNotVerifiedException e) {
            log.warn("⚠️ [LOGIN FAILED] Неверифицированный пользователь пытается войти: {}", request.getUsername());
            
            Map<String, String> response = new HashMap<>();
            response.put("error", "User not verified");
            response.put("message", "Your account is not verified. Please verify your account with Telegram bot.");
            response.put("verificationCode", e.getVerificationCode());
            response.put("status", "not_verified");
            
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            log.error("🔥 [LOGIN ERROR] Ошибка входа для пользователя {}: {}", 
                    request.getUsername(), e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred during login");
            errorResponse.put("error", e.getMessage());
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
    public ResponseEntity<?> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        try {
            // Extract username from the JWT token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Authorization header with Bearer token is required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            String token = authHeader.substring(7);
            ValidateTokenResult validationResult = authService.validateToken(token);
            
            if (!validationResult.isValid()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            String username = validationResult.getUsername();
            
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
    
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody @Valid ChangePasswordRequest request, Authentication authentication) {
        try {
            String username;
            if (authentication.getPrincipal() instanceof User) {
                username = ((User) authentication.getPrincipal()).getUsername();
            } else {
                username = authentication.getName();
            }
            
            log.info("🔑 [CHANGE PASSWORD] Получен запрос на изменение пароля для пользователя: {}", username);
            
            boolean success = authService.changePassword(
                    username, 
                    request.getCurrentPassword(), 
                    request.getNewPassword());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully. Please login again with your new password.");
            
            return ResponseEntity.ok(response);
        } catch (InvalidCredentialsException e) {
            log.warn("❌ [CHANGE PASSWORD] Ошибка изменения пароля: {}", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid credentials");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("🔥 [CHANGE PASSWORD ERROR] Непредвиденная ошибка: {}", e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred while changing password");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Проверяет валидность токена
     */
    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        log.info("Получен запрос на валидацию токена");
        Map<String, Object> response = new HashMap<>();

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("valid", false);
                response.put("message", "No token provided or invalid format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String token = authHeader.substring(7); // Убираем 'Bearer ' из заголовка
            ValidateTokenResult result = authService.validateToken(token);

            response.put("valid", result.isValid());
            response.put("username", result.getUsername());
            
            if (result.isValid()) {
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            log.error("Ошибка при валидации токена: {}", e.getMessage(), e);
            
            response.put("valid", false);
            response.put("message", "Error validating token: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 