package com.brand.backend.presentation.dto.request.discord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO для запроса привязки Discord-аккаунта
 */
@Data
public class LinkDiscordAccountRequest {
    /**
     * ID пользователя в Discord
     */
    @NotNull(message = "Discord ID обязателен")
    private Long discordId;
    
    /**
     * Имя пользователя в Discord
     */
    @NotBlank(message = "Имя пользователя Discord обязательно")
    private String discordUsername;
} 