package com.brand.backend.presentation.rest.controller.user;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.UserDTO;
import com.brand.backend.presentation.dto.response.SubscriptionInfoDTO;
import com.brand.backend.presentation.dto.response.UserStatsDTO;
import com.brand.backend.application.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userService.convertToDTO(user));
    }

    @GetMapping("/stats")
    public ResponseEntity<UserStatsDTO> getUserStats(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        UserStatsDTO stats = userService.getUserStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionInfoDTO> getUserSubscription(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        SubscriptionInfoDTO subscription = userService.getUserSubscriptionInfo(user.getId());
        return ResponseEntity.ok(subscription);
    }

    @GetMapping("/is-verified")
    public ResponseEntity<Map<String, Boolean>> isTelegramVerified(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("isVerified", user.isVerified()));
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, String>> updateUser(@RequestBody Map<String, String> request, 
                                                        @AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        
        String newUsername = request.get("newUsername");
        String newEmail = request.get("email");
        String newPhoneNumber = request.get("phoneNumber");

        if ((newUsername == null || newUsername.isBlank()) &&
                (newEmail == null || newEmail.isBlank()) &&
                (newPhoneNumber == null || newPhoneNumber.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No valid fields provided"));
        }

        userService.updateUserProfile(user, newUsername, newEmail, newPhoneNumber);
        return ResponseEntity.ok(Map.of("message", "User profile updated successfully"));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> request,
                                                            @AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (oldPassword == null || newPassword == null || oldPassword.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Old and new passwords are required"));
        }

        userService.changePassword(user, oldPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    
    @GetMapping("/contacts")
    public ResponseEntity<Map<String, String>> getUserContacts(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail() != null ? user.getEmail() : "",
                "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : ""
        ));
    }

}
