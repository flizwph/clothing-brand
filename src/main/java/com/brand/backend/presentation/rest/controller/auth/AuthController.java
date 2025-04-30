package com.brand.backend.presentation.rest.controller.auth;

import com.brand.backend.presentation.dto.request.UserLoginRequest;
import com.brand.backend.presentation.dto.request.UserRegistrationRequest;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.infrastructure.security.jwt.JwtUtil;
import com.brand.backend.application.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody @Valid UserRegistrationRequest request) {
        log.info("Получен запрос на регистрацию: {}", request.getUsername());
        Map<String, String> response = new HashMap<>();

        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setVerified(false);

            authService.registerUser(user, request.getPassword());

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

            Optional<User> userOptional = authService.authenticateUser(request.getUsername(), request.getPassword());
            Map<String, String> response = new HashMap<>();

            if (userOptional.isEmpty()) {
                log.info("❌ [LOGIN] Неверные учетные данные: {}", request.getUsername());
                response.put("message", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            User user = userOptional.get();
            log.info("✅ [USER FOUND] Пользователь {} найден в базе", request.getUsername());

            if (!user.isVerified()) {
                log.warn("⚠️ [LOGIN BLOCKED] Аккаунт {} не верифицирован!", request.getUsername());
                response.put("message", "Account not verified. Use this code in Telegram bot:");
                response.put("verificationCode", user.getVerificationCode());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            log.info("🔑 [JWT] Генерация токена для {}", request.getUsername());
            String accessToken = jwtUtil.generateAccessToken(user.getUsername());
            String refreshToken = authService.generateRefreshToken(user);

            response.put("message", "Login successful");
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            
            // Асинхронное обновление lastLogin
            CompletableFuture.runAsync(() -> {
                try {
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);
                    log.debug("🕒 Асинхронно обновлен lastLogin для {}", user.getUsername());
                } catch (Exception e) {
                    log.warn("⚠️ Не удалось обновить lastLogin: {}", e.getMessage());
                }
            });

            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(response);
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
            
            String username = authService.refreshAccessToken(refreshToken);
            String newAccessToken = jwtUtil.generateAccessToken(username);

            Map<String, String> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("❌ [REFRESH ERROR] {}", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
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
            
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            authService.logout(userOptional.get());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("🔥 [LOGOUT ERROR] Ошибка при выходе: {}", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred during logout");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
