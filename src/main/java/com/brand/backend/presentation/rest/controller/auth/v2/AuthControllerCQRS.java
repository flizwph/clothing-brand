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
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;

import com.brand.backend.application.user.service.VerificationService;
import java.util.Optional;

/**
 * Контроллер для аутентификации с использованием CQRS
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/v2")
@RequiredArgsConstructor
public class AuthControllerCQRS {

    private final AuthServiceCQRS authService;
    private final VerificationService verificationService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody @Valid UserRegistrationRequest request) {
        log.info("Получен запрос на регистрацию: {}", request.getUsername());
        
        User user = authService.registerUser(
            request.getUsername(), 
            request.getEmail(), 
            request.getPassword(), 
            request.getConfirmPassword()
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Пользователь успешно зарегистрирован");
        response.put("verificationCode", user.getVerificationCode());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());

        log.info("Регистрация успешна для пользователя: {} с email: {}", request.getUsername(), request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
     * Генерирует новый код верификации для Telegram
     */
    @PostMapping("/generate-telegram-code")
    public ResponseEntity<Map<String, String>> generateTelegramCode(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            
            if (username == null || username.isBlank()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("success", "false");
                errorResponse.put("message", "Username is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            log.info("🔄 [GENERATE TELEGRAM CODE] Генерация нового кода верификации для: {}", username);
            
            String verificationCode = verificationService.generateAndSaveVerificationCode(username);
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Новый код верификации сгенерирован");
            response.put("verificationCode", verificationCode);
            response.put("username", username);
            
            log.info("✅ [GENERATE TELEGRAM CODE] Код верификации сгенерирован для: {}", username);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("❌ [GENERATE TELEGRAM CODE] Пользователь не найден: {}", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("success", "false");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("🔥 [GENERATE TELEGRAM CODE ERROR] Ошибка генерации кода: {}", e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("success", "false");
            errorResponse.put("message", "An error occurred during code generation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Обработка OPTIONS запроса для CORS preflight
     */
    @RequestMapping(value = "/login-verified", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> loginVerifiedOptions() {
        log.info("🔧 [OPTIONS] Получен OPTIONS запрос на login-verified");
        return ResponseEntity.ok().build();
    }

    /**
     * Проверяет статус верификации пользователя (только проверка, без автологина)
     */
    @GetMapping("/verification-status")
    public ResponseEntity<Map<String, Object>> getVerificationStatus(@RequestParam("code") String verificationCode) {
        try {
            log.info("🔍 [VERIFICATION STATUS] Проверка статуса верификации по коду: {}", verificationCode);
            
            if (verificationCode == null || verificationCode.isBlank()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Verification code is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Поиск пользователя по коду
            Optional<User> userOptional = authService.findUserByVerificationCode(verificationCode);
            
            Map<String, Object> response = new HashMap<>();
            
            if (userOptional.isEmpty()) {
                log.warn("❌ [VERIFICATION STATUS] Пользователь с кодом {} не найден", verificationCode);
                response.put("success", false);
                response.put("found", false);
                response.put("message", "Неверный код верификации");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            User user = userOptional.get();
            
            response.put("success", true);
            response.put("found", true);
            response.put("verified", user.isVerified());
            response.put("username", user.getUsername());
            
            if (user.isVerified()) {
                response.put("message", "Пользователь верифицирован и готов к автологину");
                response.put("canLogin", true);
            } else {
                response.put("message", "Пользователь найден, но еще не верифицирован в Telegram");
                response.put("canLogin", false);
            }
            
            log.info("✅ [VERIFICATION STATUS] Статус для {}: verified={}", user.getUsername(), user.isVerified());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("🔥 [VERIFICATION STATUS ERROR] Ошибка при проверке статуса: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An error occurred during status check");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * DEPRECATED: Используйте /verification-status (GET) + /login-verified (POST)
     * Оставлен для обратной совместимости
     */
    @PostMapping("/check-verification")
    public ResponseEntity<Map<String, Object>> checkVerificationDeprecated(@RequestBody Map<String, String> request) {
        log.warn("⚠️ [DEPRECATED] Использован устаревший эндпоинт /check-verification. Используйте /verification-status + /login-verified");
        return loginVerified(request);
    }

    /**
     * Выполняет автологин для верифицированного пользователя по коду верификации
     */
    @PostMapping("/login-verified")
    public ResponseEntity<Map<String, Object>> loginVerified(@RequestBody Map<String, String> request) {
        try {
            String verificationCode = request.get("verificationCode");
            
            if (verificationCode == null || verificationCode.isBlank()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Verification code is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            log.info("🔍 [CHECK VERIFICATION] Проверка верификации по коду: {}", verificationCode);
            
            LoginCommandResult result = authService.checkVerificationAndLogin(verificationCode);
            Map<String, Object> response = new HashMap<>();
            
            if (!result.isSuccess()) {
                log.warn("❌ [CHECK VERIFICATION] Неверный код верификации: {}", verificationCode);
                response.put("success", false);
                response.put("message", result.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            if (result.isNeedsVerification()) {
                // Пользователь найден, но еще не верифицирован
                log.info("⏳ [CHECK VERIFICATION] Пользователь найден но не верифицирован: код {}", verificationCode);
                response.put("success", true);
                response.put("verified", false);
                response.put("message", result.getMessage());
                response.put("verificationCode", result.getVerificationCode());
                return ResponseEntity.ok(response);
            } else {
                // Пользователь верифицирован - возвращаем токены
                log.info("✅ [CHECK VERIFICATION] Пользователь верифицирован, выдаем токены: {}", result.getUser().getUsername());
                response.put("success", true);
                response.put("verified", true);
                response.put("message", result.getMessage());
                response.put("accessToken", result.getAccessToken());
                response.put("refreshToken", result.getRefreshToken());
                response.put("username", result.getUser().getUsername());
                
                return ResponseEntity.ok()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + result.getAccessToken())
                        .body(response);
            }
        } catch (Exception e) {
            log.error("🔥 [CHECK VERIFICATION ERROR] Ошибка при проверке верификации: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An error occurred during verification check");
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