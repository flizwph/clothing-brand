package com.brand.backend.presentation.dto.response.discord;

import lombok.Builder;
import lombok.Data;

/**
 * DTO для ответа о статусе привязки Discord-аккаунта
 */
@Data
@Builder
public class DiscordStatusResponse {
    /**
     * Привязан ли Discord аккаунт
     */
    private boolean linked;
    
    /**
     * Имя пользователя в Discord
     */
    private String discordUsername;
    
    /**
     * ID пользователя в Discord
     */
    private Long discordId;
} 