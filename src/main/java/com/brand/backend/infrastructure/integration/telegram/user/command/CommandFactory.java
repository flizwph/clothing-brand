package com.brand.backend.infrastructure.integration.telegram.user.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramProductService;
import com.brand.backend.infrastructure.integration.telegram.user.service.CartService;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import com.brand.backend.infrastructure.integration.telegram.user.service.UserSessionService;

/**
 * Фабрика для создания и хранения команд
 */
@Component
public class CommandFactory {
    
    private final Map<String, Command> commands = new HashMap<>();
    private final UserSessionService userSessionService;
    private final TelegramProductService telegramProductService;
    private final CartService cartService;
    private final TelegramBotService botService;
    
    /**
     * Конструктор фабрики, инициализирует команды
     */
    public CommandFactory(Set<Command> availableCommands, 
                        UserSessionService userSessionService, 
                        TelegramProductService telegramProductService,
                        CartService cartService,
                        TelegramBotService botService) {
        this.userSessionService = userSessionService;
        this.telegramProductService = telegramProductService;
        this.cartService = cartService;
        this.botService = botService;
        
        // Регистрируем команды из Spring-контекста
        if (availableCommands != null && !availableCommands.isEmpty()) {
            availableCommands.forEach(command -> commands.put(command.getCommandName(), command));
        } else {
            // Если нет команд в контексте, создаем их вручную
            registerCommands();
        }
    }
    
    /**
     * Регистрирует команды в фабрике
     */
    private void registerCommands() {
        commands.put("/start", new StartCommand());
        commands.put("/help", new HelpCommand());
        commands.put("/buy", new BuyCommand(telegramProductService));
        commands.put("/cart", new CartCommand(cartService, botService));
        commands.put("/search_user", new SearchUserCommand(userSessionService));
    }
    
    /**
     * Возвращает команду по имени
     * 
     * @param commandName имя команды
     * @return команда или null, если команда не найдена
     */
    public Command getCommand(String commandName) {
        return commands.get(commandName);
    }
    
    /**
     * Проверяет, существует ли команда с указанным именем
     * 
     * @param commandName имя команды
     * @return true, если команда существует, false в противном случае
     */
    public boolean hasCommand(String commandName) {
        return commands.containsKey(commandName);
    }
} 