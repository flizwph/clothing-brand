package com.brand.backend.infrastructure.integration.telegram.admin;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@ConditionalOnProperty(name = "admin.bot.enabled", havingValue = "true", matchIfMissing = false)
public class AdminBotConfig {

    private final AdminTelegramBot adminTelegramBot;

    public AdminBotConfig(AdminTelegramBot adminTelegramBot) {
        this.adminTelegramBot = adminTelegramBot;
    }

    @Bean(name = "adminTelegramBotsApi")
    public TelegramBotsApi adminTelegramBotsApi() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(adminTelegramBot);
        return botsApi;
    }
}
