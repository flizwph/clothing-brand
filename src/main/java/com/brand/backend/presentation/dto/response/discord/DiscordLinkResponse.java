package com.brand.backend.presentation.dto.response.discord;

import lombok.Builder;
import lombok.Data;

/**
 * DTO для ответа на запрос привязки Discord-аккаунта
 */
@Data
@Builder
public class DiscordLinkResponse {
    /**
     * Успешность операции
     */
    private boolean success;
    
    /**
     * Сообщение для пользователя
     */
    private String message;
    
    /**
     * Имя пользователя на сайте
     */
    private String username;
} 