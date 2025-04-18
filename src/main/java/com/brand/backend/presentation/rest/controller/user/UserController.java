package com.brand.backend.presentation.rest.controller.user;

import com.brand.backend.presentation.dto.request.UserDTO;
import com.brand.backend.application.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @GetMapping("/is-verified")
    public ResponseEntity<Map<String, Boolean>> isTelegramVerified() {
        return ResponseEntity.ok(Map.of("isVerified", userService.isTelegramVerified()));
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, String>> updateUser(@RequestBody Map<String, String> request) {
        String newUsername = request.get("newUsername");
        String newEmail = request.get("email");
        String newPhoneNumber = request.get("phoneNumber");

        if ((newUsername == null || newUsername.isBlank()) &&
                (newEmail == null || newEmail.isBlank()) &&
                (newPhoneNumber == null || newPhoneNumber.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No valid fields provided"));
        }

        userService.updateUserProfile(newUsername, newEmail, newPhoneNumber);
        return ResponseEntity.ok(Map.of("message", "User profile updated successfully"));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> request) {
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (oldPassword == null || newPassword == null || oldPassword.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Old and new passwords are required"));
        }

        userService.changePassword(oldPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    @GetMapping("/contacts")
    public ResponseEntity<Map<String, String>> getUserContacts() {
        // Получаем полный объект UserDTO, который содержит актуальные данные из профиля
        UserDTO userDTO = userService.getCurrentUser();
        // Возвращаем только поля email и phoneNumber (если они отсутствуют, можно вернуть пустую строку)
        return ResponseEntity.ok(Map.of(
                "email", userDTO.getEmail() != null ? userDTO.getEmail() : "",
                "phoneNumber", userDTO.getPhoneNumber() != null ? userDTO.getPhoneNumber() : ""
        ));
    }

}
