package com.brand.backend.dtos;

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
    private String password;
    private String role;
    private boolean isActive;
    private Long telegramId; // ID Telegram-аккаунта
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
