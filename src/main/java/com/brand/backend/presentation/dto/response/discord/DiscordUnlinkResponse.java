package com.brand.backend.presentation.dto.response.discord;

import lombok.Builder;
import lombok.Data;

/**
 * DTO для ответа на запрос отвязки Discord-аккаунта
 */
@Data
@Builder
public class DiscordUnlinkResponse {
    /**
     * Успешность операции
     */
    private boolean success;
    
    /**
     * Сообщение для пользователя
     */
    private String message;
}