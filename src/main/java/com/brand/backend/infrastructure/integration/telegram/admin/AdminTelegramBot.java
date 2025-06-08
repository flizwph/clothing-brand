package com.brand.backend.infrastructure.integration.telegram.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "admin.bot.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class AdminTelegramBot extends TelegramLongPollingBot {

    @Value("${admin.bot.username}")
    private String botUsername;
    
    @Value("${admin.bot.adminIds:}")
    private String allowedAdminIdsString;

    public AdminTelegramBot(@Value("${admin.bot.token}") String botToken) {
        super(botToken);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("Received update: {}", update);
    }
    
    public List<Long> getAllowedAdminIds() {
        if (allowedAdminIdsString == null || allowedAdminIdsString.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(allowedAdminIdsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
    }
}
