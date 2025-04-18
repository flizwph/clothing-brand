package com.brand.backend.presentation.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class UserDTO {
    private Long id;
    private String username;
    // Не включаем поле пароля для безопасности
    private String role;
    private boolean active;
    private Long telegramId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Новые поля, отражающие актуальную структуру таблицы
    private String verificationCode;
    private boolean verified;
    private Long discordId;
    private LocalDateTime lastLogin;
    private String telegramUsername;
    private String discordUsername;
    private String vkUsername;
    private boolean linkedDiscord;
    private boolean linkedVkontakte;
    private String email;
    private String phoneNumber;
}
