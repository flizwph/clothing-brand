package com.brand.backend.infrastructure.integration.telegram.admin.handlers;

import com.brand.backend.infrastructure.integration.telegram.admin.keyboards.AdminKeyboards;
import com.brand.backend.infrastructure.integration.telegram.admin.service.AdminBotService;
import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Обработчик команд, связанных с пользователями
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserHandler {

    private final AdminBotService adminBotService;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Отправляет сообщение со списком пользователей
     */
    public SendMessage handleUserList(String chatId) {
        List<User> users = adminBotService.getAllUsers();
        
        if (users.isEmpty()) {
            return createMessage(chatId, "Пользователи не найдены.");
        }
        
        StringBuilder message = new StringBuilder("*👥 Список пользователей:*\n\n");
        
        for (int i = 0; i < Math.min(20, users.size()); i++) {
            User user = users.get(i);
            message.append(i + 1).append(". *").append(user.getUsername()).append("*");
            
            if (user.getEmail() != null) {
                message.append(" (").append(user.getEmail()).append(")");
            }
            
            message.append("\n");
            
            List<Order> userOrders = adminBotService.getOrdersByUser(user);
            message.append("📝 Заказов: ").append(userOrders.size()).append("\n");
            
            if (user.getTelegramUsername() != null) {
                message.append("🔗 Telegram: @").append(user.getTelegramUsername()).append("\n");
            }
            
            message.append("/user_").append(user.getId()).append(" - подробная информация\n\n");
        }
        
        if (users.size() > 20) {
            message.append("...и еще ").append(users.size() - 20).append(" пользователей.\n");
            message.append("Используйте поиск для нахождения конкретного пользователя.");
        }
        
        return createMessage(chatId, message.toString(), createUserListKeyboard());
    }
    
    /**
     * Отправляет сообщение с детальной информацией о пользователе
     */
    public SendMessage handleUserDetails(String chatId, Long userId) {
        User user = adminBotService.getUserById(userId);
        
        if (user == null) {
            return createMessage(chatId, "Пользователь не найден.");
        }
        
        StringBuilder message = new StringBuilder("*👤 Пользователь: ").append(escapeMarkdown(user.getUsername())).append("*\n\n");
        
        message.append("*ID:* ").append(user.getId()).append("\n");
        message.append("*Роль:* ").append(user.getRole()).append("\n");
        message.append("*Email:* ").append(user.getEmail() != null ? user.getEmail() : "-").append("\n");
        message.append("*Телефон:* ").append(user.getPhoneNumber() != null ? user.getPhoneNumber() : "-").append("\n\n");
        
        message.append("*Социальные сети:*\n");
        message.append("*Telegram ID:* ").append(user.getTelegramId() != null ? user.getTelegramId() : "-").append("\n");
        message.append("*Telegram:* ").append(user.getTelegramUsername() != null ? "@" + escapeMarkdown(user.getTelegramUsername()) : "-").append("\n");
        message.append("*Discord:* ").append(escapeMarkdown(user.getDiscordUsername())).append("\n");
        message.append("*VK:* ").append(escapeMarkdown(user.getVkUsername())).append("\n\n");
        
        message.append("*Статус:*\n");
        message.append("*Активен:* ").append(user.isActive() ? "✅" : "❌").append("\n");
        message.append("*Верифицирован:* ").append(user.isVerified() ? "✅" : "❌").append("\n");
        message.append("*Discord привязан:* ").append(user.isLinkedDiscord() ? "✅" : "❌").append("\n");
        message.append("*VK привязан:* ").append(user.isLinkedVkontakte() ? "✅" : "❌").append("\n\n");
        
        message.append("*Дата регистрации:* ").append(user.getCreatedAt().format(DATETIME_FORMATTER)).append("\n");
        
        if (user.getLastLogin() != null) {
            message.append("*Последний вход:* ").append(user.getLastLogin().format(DATETIME_FORMATTER)).append("\n");
        }
        
        return createMessage(chatId, message.toString(), createUserDetailsKeyboard(user));
    }
    
    /**
     * Выполняет поиск пользователей по заданному запросу
     */
    public SendMessage handleUserSearch(String chatId, String query) {
        return handleUserSearch(chatId, query, "general");
    }

    /**
     * Выполняет поиск пользователей по заданному запросу и типу поиска
     */
    public SendMessage handleUserSearch(String chatId, String query, String searchType) {
        log.info("Поиск пользователей по запросу: {}, тип поиска: {}", query, searchType);
        
        List<User> users = adminBotService.getAllUsers();
        List<User> filteredUsers = users.stream()
                .filter(user -> matchesUser(user, query, searchType))
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .collect(Collectors.toList());
        
        if (filteredUsers.isEmpty()) {
            return createMessage(chatId, "Пользователи не найдены по запросу: " + query);
        }
        
        StringBuilder messageText = new StringBuilder();
        messageText.append("🔍 *Результаты поиска по запросу:* ").append(query).append("\n\n");
        
        int count = 0;
        for (User user : filteredUsers) {
            count++;
            messageText.append("👤 *").append(user.getUsername()).append("*\n");
            messageText.append("ID: `").append(user.getId()).append("`\n");
            messageText.append("Email: ").append(user.getEmail()).append("\n");
            messageText.append("Телефон: ").append(user.getPhoneNumber() != null ? user.getPhoneNumber() : "не указан").append("\n");
            
            if (count < filteredUsers.size()) {
                messageText.append("\n");
            }
            
            // Ограничиваем количество выводимых пользователей
            if (count >= 10) {
                messageText.append("\n_...и еще ")
                        .append(filteredUsers.size() - 10)
                        .append(" пользователей_");
                break;
            }
        }
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        for (int i = 0; i < Math.min(5, filteredUsers.size()); i++) {
            User user = filteredUsers.get(i);
            List<InlineKeyboardButton> row = new ArrayList<>();
            
            InlineKeyboardButton viewButton = createButton(
                    "👁️ " + user.getUsername(),
                    "viewUser:" + user.getId()
            );
            row.add(viewButton);
            keyboard.add(row);
        }
        
        // Добавляем кнопку "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(createButton("◀️ Назад в меню", "menu:main"));
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        return createMessage(chatId, messageText.toString(), keyboardMarkup);
    }
    
    /**
     * Создаёт клавиатуру для списка пользователей
     */
    private InlineKeyboardMarkup createUserListKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("🔍 Поиск пользователя", "searchUser"));
        rows.add(row1);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    
    /**
     * Создаёт клавиатуру для детальной информации о пользователе
     */
    private InlineKeyboardMarkup createUserDetailsKeyboard(User user) {
        // Используем общий метод из AdminKeyboards для создания клавиатуры профиля пользователя
        return AdminKeyboards.createUserProfileKeyboard(user.getId(), user.isActive());
    }
    
    /**
     * Проверяет, соответствует ли пользователь поисковому запросу
     */
    private boolean matchesUser(User user, String query) {
        return matchesUser(user, query, "general");
    }

    /**
     * Проверяет, соответствует ли пользователь поисковому запросу по определенному типу поиска
     */
    private boolean matchesUser(User user, String query, String searchType) {
        if (query == null || query.isEmpty()) {
            return false;
        }
        
        String lowerQuery = query.toLowerCase();
        
        switch (searchType) {
            case "name":
                return user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerQuery);
            case "email":
                return user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerQuery);
            case "phone":
                return user.getPhoneNumber() != null && user.getPhoneNumber().toLowerCase().contains(lowerQuery);
            case "general":
            default:
                return (user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerQuery)) ||
                       (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerQuery)) ||
                       (user.getPhoneNumber() != null && user.getPhoneNumber().toLowerCase().contains(lowerQuery)) ||
                       (user.getId() != null && user.getId().toString().equals(lowerQuery));
        }
    }
    
    /**
     * Обрабатывает запрос на поиск пользователя
     */
    public SendMessage handleUserSearchRequest(String chatId) {
        String text = """
                *🔍 Поиск пользователя*
                
                Введите имя пользователя, email или телефон для поиска.
                
                Например: /user_search Иван
                Или: /user_search example@mail.com
                Или: /user_search +79123456789
                """;
        
        return createMessage(chatId, text);
    }
    
    /**
     * Создаёт объект сообщения
     */
    private SendMessage createMessage(String chatId, String text) {
        return createMessage(chatId, text, true);
    }
    
    /**
     * Создаёт объект сообщения с возможностью указать режим форматирования
     */
    private SendMessage createMessage(String chatId, String text, boolean useMarkdown) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (useMarkdown) {
            message.setParseMode("Markdown");
        }
        return message;
    }
    
    /**
     * Создаёт объект сообщения с клавиатурой
     */
    private SendMessage createMessage(String chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard) {
        return createMessage(chatId, text, keyboard, true);
    }
    
    /**
     * Создаёт объект сообщения с клавиатурой и возможностью указать режим форматирования
     */
    private SendMessage createMessage(String chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard, boolean useMarkdown) {
        SendMessage message = createMessage(chatId, text, useMarkdown);
        message.setReplyMarkup(keyboard);
        return message;
    }
    
    /**
     * Создаёт кнопку с текстом и callback-данными
     */
    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
    
    /**
     * Обрабатывает переключение статуса пользователя (активация/деактивация)
     */
    public SendMessage handleToggleUserStatus(String chatId, Long userId, Integer messageId) {
        User user = adminBotService.getUserById(userId);
        
        if (user == null) {
            return createMessage(chatId, "Пользователь не найден.");
        }
        
        // Инвертируем статус активности
        boolean newStatus = !user.isActive();
        adminBotService.updateUserActiveStatus(userId, newStatus);
        
        String statusText = newStatus ? "активирован ✅" : "деактивирован ❌";
        String message = "*👤 Пользователь " + escapeMarkdown(user.getUsername()) + " " + statusText + "*\n\n" +
                "Статус успешно обновлен.";
        
        return createMessage(chatId, message, createUserDetailsKeyboard(user));
    }
    
    /**
     * Отображает форму для поиска пользователей
     */
    public SendMessage handleSearchUser(String chatId) {
        log.info(">> Обработка запроса на поиск пользователя");
        try {
        String text = """
                    🔍 *Поиск пользователя*
                    
                    Введите или скопируйте одну из команд ниже и добавьте параметры поиска:
                    
                    `/search_user имя_пользователя` - поиск по имени
                    `/email адрес@почты.com` - поиск по email
                    `/phone +79991234567` - поиск по телефону
                    
                    💡 Команды можно скопировать, нажав на них.
                    """;
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.setParseMode("Markdown");
            
            // Добавляем клавиатуру для возврата
            message.setReplyMarkup(AdminKeyboards.createUserSearchKeyboard());
            
            return message;
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса на поиск пользователя: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage(), 
                    AdminKeyboards.createBackKeyboard("menu:users"));
        } finally {
            log.info("<< Завершение обработки запроса на поиск пользователя");
        }
    }

    /**
     * Обрабатывает запрос на получение списка пользователей
     */
    public SendMessage handleListUsers(String chatId) {
        log.info(">> Обработка запроса на получение списка пользователей");
        try {
            List<User> users = adminBotService.getAllUsers();
            
            if (users.isEmpty()) {
                log.warn("Список пользователей пуст");
                return createMessage(
                    chatId, 
                    "Пользователи не найдены.", 
                    AdminKeyboards.createBackKeyboard("menu:main")
                );
            }
            
            log.info("Получен список пользователей: {} записей", users.size());
            
            StringBuilder message = new StringBuilder("*👥 Список пользователей:*\n\n");
            
            for (int i = 0; i < Math.min(20, users.size()); i++) {
                User user = users.get(i);
                // Избегаем проблем с Markdown, экранируя имя пользователя
                message.append(i + 1).append(". *").append(escapeMarkdown(user.getUsername())).append("*");
                
                if (user.getEmail() != null) {
                    // Для email не используем форматирование Markdown, чтобы избежать проблем со спецсимволами
                    message.append(" (").append(user.getEmail()).append(")");
                }
                
                message.append("\n");
                
                List<Order> userOrders = adminBotService.getOrdersByUser(user);
                message.append("📝 Заказов: ").append(userOrders.size()).append("\n");
                
                if (user.getTelegramUsername() != null) {
                    message.append("🔗 Telegram: @").append(user.getTelegramUsername()).append("\n");
                }
                
                message.append("/user_").append(user.getId()).append(" - подробная информация\n\n");
            }
            
            if (users.size() > 20) {
                message.append("...и еще ").append(users.size() - 20).append(" пользователей.\n");
                message.append("Используйте поиск для нахождения конкретного пользователя.");
            }
            
            // Создаем клавиатуру с кнопками поиска и возврата в меню
            InlineKeyboardMarkup keyboardMarkup = AdminKeyboards.createUsersMenu();
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message.toString());
            sendMessage.setParseMode("Markdown");
            sendMessage.setReplyMarkup(keyboardMarkup);
            
            return sendMessage;
        } catch (Exception e) {
            log.error("Ошибка при получении списка пользователей: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка при получении списка пользователей: " + e.getMessage(),
                    AdminKeyboards.createBackKeyboard("menu:main"));
        } finally {
            log.info("<< Завершение обработки запроса на получение списка пользователей");
        }
    }

    /**
     * Обрабатывает запрос на поиск пользователя по email
     */
    public SendMessage handleSearchUserByEmail(String chatId) {
        log.info(">> Обработка запроса на поиск пользователя по email");
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("📧 *Поиск пользователя по email*\n\n" +
                    "Введите или скопируйте команду ниже и добавьте email:\n\n" +
                    "`/email адрес@почты.com`\n\n" +
                    "💡 Команду можно скопировать, нажав на неё.");
            message.setParseMode("Markdown");
            
            // Добавляем клавиатуру для возврата
            message.setReplyMarkup(AdminKeyboards.createBackKeyboard("menu:users"));
            
            return message;
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса на поиск пользователя по email: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage(), 
                    AdminKeyboards.createBackKeyboard("menu:users"));
        } finally {
            log.info("<< Завершение обработки запроса на поиск пользователя по email");
        }
    }

    /**
     * Обрабатывает запрос на поиск пользователя по телефону
     */
    public SendMessage handleSearchUserByPhone(String chatId) {
        log.info(">> Обработка запроса на поиск пользователя по телефону");
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("📱 *Поиск пользователя по телефону*\n\n" +
                    "Введите или скопируйте команду ниже и добавьте номер телефона:\n\n" +
                    "`/phone +79991234567`\n\n" +
                    "💡 Команду можно скопировать, нажав на неё.");
            message.setParseMode("Markdown");
            
            // Добавляем клавиатуру для возврата
            message.setReplyMarkup(AdminKeyboards.createBackKeyboard("menu:users"));
            
            return message;
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса на поиск пользователя по телефону: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage(), 
                    AdminKeyboards.createBackKeyboard("menu:users"));
        } finally {
            log.info("<< Завершение обработки запроса на поиск пользователя по телефону");
        }
    }

    /**
     * Формирует сообщение со списком пользователей
     */
    private SendMessage createUsersListMessage(String chatId, List<User> users, String title) {
        if (users == null || users.isEmpty()) {
            return createMessage(chatId, "*" + escapeMarkdown(title) + "*\n\nНет пользователей для отображения.",
                    AdminKeyboards.createBackKeyboard("menu:main"));
        }
        StringBuilder message = new StringBuilder();
        message.append("*").append(escapeMarkdown(title)).append("*\n\n");
        
        for (int i = 0; i < Math.min(20, users.size()); i++) {
            User user = users.get(i);
            message.append(i + 1).append(". *").append(escapeMarkdown(user.getUsername())).append("*\n");
            message.append("ID: ").append(user.getId()).append("\n");
            
            if (user.getEmail() != null) {
                message.append("Email: ").append(user.getEmail()).append("\n");
            }
            
            if (user.getPhoneNumber() != null) {
                message.append("Телефон: ").append(user.getPhoneNumber()).append("\n");
            }
            
            message.append("/user_").append(user.getId()).append(" - подробная информация\n\n");
        }
        
        if (users.size() > 20) {
            message.append("...и еще ").append(users.size() - 20).append(" пользователей.\n");
        }
        
        // Добавляем клавиатуру с меню пользователей
        InlineKeyboardMarkup keyboardMarkup = AdminKeyboards.createUsersMenu();
        
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message.toString());
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(keyboardMarkup);
        
        return sendMessage;
    }

    /**
     * Обрабатывает запрос на поиск пользователя по имени
     */
    public SendMessage handleSearchUserByName(String chatId) {
        log.info(">> Обработка запроса на поиск пользователя по имени");
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("👤 *Поиск пользователя по имени*\n\n" +
                    "Введите или скопируйте команду ниже и добавьте имя пользователя:\n\n" +
                    "`/name Имя_пользователя`\n\n" +
                    "💡 Команду можно скопировать, нажав на неё.");
            message.setParseMode("Markdown");
            
            // Добавляем клавиатуру для возврата
            message.setReplyMarkup(AdminKeyboards.createBackKeyboard("menu:users"));
            
            return message;
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса на поиск пользователя по имени: {}", e.getMessage());
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает поиск пользователя по имени
     */
    public SendMessage handleUserSearchByName(String chatId, String name) {
        log.info(">> Обработка поиска пользователя по имени: {}", name);
        try {
            List<User> users = adminBotService.findUsersByUsername(name);
            
            if (users.isEmpty()) {
                return createMessage(
                    chatId, 
                    "Пользователи с именем содержащим \"" + name + "\" не найдены.", 
                    AdminKeyboards.createBackKeyboard("menu:users")
                );
            }
            
            return createUsersListMessage(chatId, users, "Результаты поиска по имени: " + name);
        } catch (Exception e) {
            log.error("Ошибка при поиске пользователя по имени: {}", e.getMessage());
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        }
    }

    /**
     * Экранирует специальные символы Markdown
     * 
     * @param text текст для экранирования
     * @return экранированный текст
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "-";
        }
        return text.replace("*", "\\*")
                  .replace("_", "\\_")
                  .replace("`", "\\`")
                  .replace("[", "\\[");
    }
} 