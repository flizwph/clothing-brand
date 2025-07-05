package com.brand.backend.presentation.rest.controller.discord;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.application.user.service.UserService;
import com.brand.backend.application.user.service.VerificationService;
import com.brand.backend.presentation.dto.request.discord.LinkDiscordAccountRequest;
import com.brand.backend.presentation.dto.response.ApiResponse;
import com.brand.backend.presentation.dto.response.discord.DiscordLinkResponse;
import com.brand.backend.presentation.dto.response.discord.DiscordStatusResponse;
import com.brand.backend.presentation.dto.response.discord.DiscordUnlinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/discord")
@RequiredArgsConstructor
public class DiscordController {

    private final UserService userService;
    private final VerificationService verificationService;
    private final UserRepository userRepository;

    /**
     * Отвязка Discord-аккаунта для авторизованного пользователя
     * 
     * @param user аутентифицированный пользователь
     * @return результат отвязки
     */
    @PostMapping("/unlink")
    public ResponseEntity<ApiResponse<DiscordUnlinkResponse>> unlinkDiscordAccount(
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        log.info("Запрос на отвязку Discord-аккаунта от пользователя: {}", username);
        
        try {
            boolean success = userService.unlinkDiscordAccount(username);
            
            DiscordUnlinkResponse response = DiscordUnlinkResponse.builder()
                    .success(success)
                    .message(success ? "Discord аккаунт успешно отвязан" : "Discord аккаунт не был привязан")
                    .build();
            
            return ResponseEntity.ok(new ApiResponse<>(response));
        } catch (Exception e) {
            log.error("Ошибка при отвязке Discord аккаунта: {}", e.getMessage(), e);
            
            DiscordUnlinkResponse response = DiscordUnlinkResponse.builder()
                    .success(false)
                    .message("Произошла ошибка при отвязке Discord аккаунта: " + e.getMessage())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(response));
        }
    }

    /**
     * Получение статуса привязки Discord-аккаунта для авторизованного пользователя
     * 
     * @param user аутентифицированный пользователь
     * @return статус привязки Discord-аккаунта
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<DiscordStatusResponse>> getDiscordStatus(
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        log.info("Запрос на получение статуса Discord-аккаунта от пользователя: {}", username);
        
        DiscordStatusResponse response = DiscordStatusResponse.builder()
                .linked(user.isDiscordVerified())
                .discordUsername(user.getDiscordUsername())
                .discordId(user.getDiscordId())
                .build();
        
        return ResponseEntity.ok(new ApiResponse<>(response));
    }
    
    /**
     * Привязка Discord-аккаунта для авторизованного пользователя
     * 
     * @param user аутентифицированный пользователь
     * @param request данные Discord-аккаунта
     * @return результат привязки
     */
    @PostMapping("/link")
    public ResponseEntity<ApiResponse<DiscordLinkResponse>> linkDiscordAccount(
            Authentication authentication,
            @RequestBody LinkDiscordAccountRequest request) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        log.info("Запрос на привязку Discord-аккаунта от пользователя {}: discordId={}, discordUsername={}", 
                username, request.getDiscordId(), request.getDiscordUsername());
        
        try {
            // Проверка, не привязан ли уже этот Discord ID к другому аккаунту
            Optional<User> existingUser = userRepository.findByDiscordId(request.getDiscordId());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                log.warn("Попытка привязать уже привязанный Discord ID: {}", request.getDiscordId());
                
                DiscordLinkResponse response = DiscordLinkResponse.builder()
                        .success(false)
                        .message("Этот Discord аккаунт уже привязан к другому пользователю")
                        .build();
                
                return ResponseEntity.ok(new ApiResponse<>(response));
            }
            
            // Привязка Discord аккаунта
            boolean success = userService.linkDiscordAccount(
                    username, 
                    request.getDiscordId(), 
                    request.getDiscordUsername()
            );
            
            DiscordLinkResponse response = DiscordLinkResponse.builder()
                    .success(success)
                    .message(success ? "Discord аккаунт успешно привязан" : "Ошибка при привязке Discord аккаунта")
                    .username(username)
                    .build();
            
            log.info("Discord аккаунт {} успешно привязан к пользователю: {}", 
                    request.getDiscordUsername(), username);
            
            return ResponseEntity.ok(new ApiResponse<>(response));
        } catch (Exception e) {
            log.error("Ошибка при привязке Discord аккаунта: {}", e.getMessage(), e);
            
            DiscordLinkResponse response = DiscordLinkResponse.builder()
                    .success(false)
                    .message("Произошла ошибка при привязке Discord аккаунта: " + e.getMessage())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(response));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyDiscordAccount(
            @RequestBody VerificationRequest request) {
        
        try {
            Optional<User> userOpt = userRepository.findByDiscordVerificationCode(request.getVerificationCode());
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Неверный код верификации"
                ));
            }
            
            User user = userOpt.get();
            
            if (request.getDiscordId() != null && !request.getDiscordId().isEmpty()) {
                Optional<User> existingUser = userRepository.findByDiscordId(Long.parseLong(request.getDiscordId()));
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Этот Discord аккаунт уже привязан к другому пользователю"
                    ));
                }
                
                user.setDiscordId(Long.parseLong(request.getDiscordId()));
            }
            
            user.setDiscordVerified(true);
            user.setDiscordVerificationCode(null);
            userRepository.save(user);
            
            log.info("Discord аккаунт {} успешно привязан к пользователю {}", 
                    request.getDiscordId(), user.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Discord аккаунт успешно привязан",
                "username", user.getUsername()
            ));
            
        } catch (Exception e) {
            log.error("Ошибка при верификации Discord: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Ошибка при верификации"
            ));
        }
    }

    @PostMapping("/verify-by-code")
    public ResponseEntity<Map<String, Object>> verifyByCode(
            @RequestBody Map<String, String> request) {
        
        String verificationCode = request.get("verificationCode");
        String discordId = request.get("discordId");
        
        if (discordId == null || discordId.isEmpty()) {
            discordId = "default_" + verificationCode.hashCode();
        }
        
        Optional<User> userOpt = userRepository.findByDiscordVerificationCode(verificationCode);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Код верификации не найден или истек"
            ));
        }
        
        User user = userOpt.get();
        user.setDiscordId(Long.parseLong(discordId));
        user.setDiscordVerified(true);
        user.setDiscordVerificationCode(null);
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Discord аккаунт успешно привязан",
            "username", user.getUsername(),
            "discordId", discordId
        ));
    }

    @GetMapping("/generate-code")
    public ResponseEntity<Map<String, String>> generateVerificationCode(
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        
        String username = authentication.getName();
        
        String discordId = null;
        if (request != null) {
            discordId = request.get("discordId");
        }
        
        String verificationCode = verificationService.generateDiscordVerificationCode(username, discordId);
        
        return ResponseEntity.ok(Map.of(
            "verificationCode", verificationCode,
            "message", "Код верификации создан. Используйте его в Discord боте."
        ));
    }

    /**
     * Проверяет статус верификации пользователя Discord
     *
     * @param request запрос, содержащий discordId и discordUsername
     * @return статус верификации и информация о пользователе
     */
    @PostMapping("/check-status")
    public ResponseEntity<Map<String, Object>> checkDiscordVerificationStatus(
            @RequestBody Map<String, String> request) {
        
        String discordId = request.get("discordId");
        String discordUsername = request.get("discordUsername");
        
        log.info("Запрос на проверку статуса верификации Discord: discordId={}, discordUsername={}", 
                discordId, discordUsername);
        
        try {
            // Поиск пользователя по привязанному Discord ID
            Optional<User> userOpt = userRepository.findByDiscordId(Long.parseLong(discordId));
            
            Map<String, Object> response = new HashMap<>();
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                response.put("success", true);
                response.put("message", "Ваш аккаунт успешно привязан к сайту! Имя пользователя: " + user.getUsername());
                response.put("username", user.getUsername());
                response.put("telegramHandle", user.getTelegramUsername() != null ? "@" + user.getTelegramUsername() : "");
                
                log.info("Пользователь с Discord ID {} верифицирован. Username: {}", discordId, user.getUsername());
            } else {
                response.put("success", false);
                response.put("message", "Аккаунт не найден. Пожалуйста, завершите верификацию на сайте.");
                
                log.info("Пользователь с Discord ID {} не найден", discordId);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при проверке статуса верификации Discord: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Произошла ошибка при проверке статуса: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/check-user/{discordId}")
    public ResponseEntity<Map<String, Object>> checkUserByDiscordId(@PathVariable String discordId) {
        Optional<User> userOpt = userRepository.findByDiscordId(Long.parseLong(discordId));
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                "found", true,
                "username", user.getUsername(),
                "verified", user.isDiscordVerified()
            ));
        }
        
        return ResponseEntity.ok(Map.of(
            "found", false,
            "message", "Пользователь с таким Discord ID не найден"
        ));
    }

    public static class VerificationRequest {
        private String verificationCode;
        private String discordId;

        public String getVerificationCode() { return verificationCode; }
        public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
        public String getDiscordId() { return discordId; }
        public void setDiscordId(String discordId) { this.discordId = discordId; }
    }
}