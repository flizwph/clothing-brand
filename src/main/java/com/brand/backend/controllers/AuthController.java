package com.brand.backend.controllers;

import com.brand.backend.dtos.UserLoginRequest;
import com.brand.backend.dtos.UserRegistrationRequest;
import com.brand.backend.models.User;
import com.brand.backend.repositories.UserRepository;
import com.brand.backend.security.JwtUtil;
import com.brand.backend.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

        log.info("📥 [LOGIN] Получен запрос на вход: {}", request.getUsername());

        Optional<User> userOptional = authService.authenticateUser(request.getUsername(), request.getPassword());
        Map<String, String> response = new HashMap<>();

        if (userOptional.isEmpty()) {
            log.info("📥 [LOGIN] Получен запрос на вход: {}", request.getUsername());
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

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshAccessToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        String username = authService.refreshAccessToken(refreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(username);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        User user = userRepository.findByUsername(username).orElseThrow();
        authService.logout(user);
        return ResponseEntity.noContent().build();
    }
}
