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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
            @AuthenticationPrincipal User user) {
        
        log.info("Запрос на отвязку Discord-аккаунта от пользователя: {}", user.getUsername());
        
        try {
            boolean success = userService.unlinkDiscordAccount(user.getUsername());
            
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
            @AuthenticationPrincipal User user) {
        
        log.info("Запрос на получение статуса Discord-аккаунта от пользователя: {}", user.getUsername());
        
        DiscordStatusResponse response = DiscordStatusResponse.builder()
                .linked(user.isLinkedDiscord())
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
            @AuthenticationPrincipal User user,
            @Valid @RequestBody LinkDiscordAccountRequest request) {
        
        log.info("Запрос на привязку Discord-аккаунта от пользователя {}: discordId={}, discordUsername={}", 
                user.getUsername(), request.getDiscordId(), request.getDiscordUsername());
        
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
                    user.getUsername(), 
                    request.getDiscordId(), 
                    request.getDiscordUsername()
            );
            
            DiscordLinkResponse response = DiscordLinkResponse.builder()
                    .success(success)
                    .message(success ? "Discord аккаунт успешно привязан" : "Ошибка при привязке Discord аккаунта")
                    .username(user.getUsername())
                    .build();
            
            log.info("Discord аккаунт {} успешно привязан к пользователю: {}", 
                    request.getDiscordUsername(), user.getUsername());
            
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
            @RequestBody Map<String, String> request) {
        
        String verificationCode = request.get("code");
        String discordUsername = request.get("discordUsername");
        
        // Проверяем наличие параметра discordId в запросе
        Long discordId = null;
        if (request.containsKey("discordId")) {
            discordId = Long.parseLong(request.get("discordId"));
        } else {
            // Если discordId не указан, используем значение по умолчанию или получаем из имени пользователя
            // Например, можно использовать хеш от имени пользователя
            discordId = (long) discordUsername.hashCode();
        }
        
        log.info("Получен запрос на верификацию Discord аккаунта: code={}, discordId={}, discordUsername={}", 
                verificationCode, discordId, discordUsername);
        
        try {
            // Ищем пользователя по коду верификации
            User user = verificationService.verifyCode(verificationCode);
            if (user == null) {
                log.warn("Неверный код верификации: {}", verificationCode);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Неверный код верификации"
                ));
            }
            
            // Привязываем Discord аккаунт
            userService.linkDiscordAccount(user.getUsername(), discordId, discordUsername);
            
            log.info("Discord аккаунт успешно привязан: username={}, discordId={}, discordUsername={}", 
                    user.getUsername(), discordId, discordUsername);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Discord аккаунт успешно привязан"
            ));
        } catch (Exception e) {
            log.error("Ошибка при верификации Discord аккаунта: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Ошибка сервера при верификации: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/generate-code")
    public ResponseEntity<Map<String, String>> generateVerificationCode() {
        try {
            String username = userService.getAuthenticatedUsername();
            String code = verificationService.generateAndSaveVerificationCode(username);
            
            log.info("Сгенерирован код верификации Discord для: {}", username);
            
            return ResponseEntity.ok(Map.of(
                "code", code,
                "message", "Используйте этот код в Discord боте с командой !link"
            ));
        } catch (Exception e) {
            log.error("Ошибка при генерации кода верификации", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Не удалось сгенерировать код верификации"
            ));
        }
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
}