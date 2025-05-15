package com.brand.backend.presentation.rest.controller.auth.v2.password;

import com.brand.backend.application.auth.core.exception.UserNotFoundException;
import com.brand.backend.application.auth.service.facade.AuthServiceCQRS;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для управления процессом сброса пароля
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/v2/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final AuthServiceCQRS authService;

    /**
     * Запрос на восстановление пароля
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody @Valid PasswordResetRequestDto requestDto) {
        try {
            log.info("Получен запрос на восстановление пароля для: {}", requestDto.getEmail());
            
            String result = authService.initiatePasswordReset(requestDto.getEmail());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            log.warn("Запрос на восстановление пароля для несуществующего email: {}", requestDto.getEmail());
            // Не сообщаем пользователю, что email не существует (по соображениям безопасности)
            Map<String, String> response = new HashMap<>();
            response.put("message", "If your email is registered, you will receive a password reset link");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при запросе восстановления пароля: {}", e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred during password reset request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Подтверждение сброса пароля с новым паролем
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(@RequestBody @Valid PasswordResetConfirmDto confirmDto) {
        try {
            log.info("Получен запрос на подтверждение сброса пароля");
            
            boolean success = authService.completePasswordReset(
                    confirmDto.getUsername(), 
                    confirmDto.getToken(), 
                    confirmDto.getNewPassword());
            
            if (!success) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Invalid or expired reset token");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password has been reset successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при подтверждении сброса пароля: {}", e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred during password reset confirmation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * DTO для запроса сброса пароля
     */
    @Data
    public static class PasswordResetRequestDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        private String email;
    }

    /**
     * DTO для подтверждения сброса пароля
     */
    @Data
    public static class PasswordResetConfirmDto {
        @NotBlank(message = "Username is required")
        private String username;
        
        @NotBlank(message = "Token is required")
        private String token;
        
        @NotBlank(message = "New password is required")
        private String newPassword;
    }
} 