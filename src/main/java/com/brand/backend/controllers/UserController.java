package com.brand.backend.controllers;

import com.brand.backend.models.User;
import com.brand.backend.repositories.UserRepository;
import com.brand.backend.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    // ✅ Получение текущего пользователя
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        log.info("📌 [USER PROFILE] Запрос профиля пользователя: {}", username);
        User user = userRepository.findByUsername(username).orElseThrow();

        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("telegramUsername", user.getTelegramUsername() != null ? user.getTelegramUsername() : "Not linked");
        response.put("discordUsername", user.getDiscordUsername() != null ? user.getDiscordUsername() : "Not linked");
        response.put("vkUsername", user.getVkUsername() != null ? user.getVkUsername() : "Not linked");
        response.put("isLinkedTelegram", user.isVerified());
        response.put("isLinkedDiscord", user.isLinkedDiscord());
        response.put("isLinkedVkontakte", user.isLinkedVkontakte());

        return ResponseEntity.ok(response);
    }

    // ✅ Изменение имени пользователя (принимает JSON)
    @PutMapping("/update")
    public ResponseEntity<Map<String, String>> updateUsername(@RequestBody Map<String, String> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        String newUsername = request.get("newUsername");
        if (newUsername == null || newUsername.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New username is required"));
        }

        log.info("✏️ [UPDATE USERNAME] {} → {}", currentUsername, newUsername);
        userService.updateUsername(currentUsername, newUsername);

        return ResponseEntity.ok(Map.of("message", "Username updated successfully"));
    }

    // ✅ Смена пароля (принимает JSON)
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (oldPassword == null || newPassword == null || oldPassword.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Old and new passwords are required"));
        }

        log.info("🔑 [CHANGE PASSWORD] Пользователь: {}", username);
        userService.changePassword(username, oldPassword, newPassword);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
