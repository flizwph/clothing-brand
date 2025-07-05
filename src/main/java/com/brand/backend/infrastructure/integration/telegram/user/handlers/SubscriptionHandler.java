package com.brand.backend.infrastructure.integration.telegram.user.handlers;

import com.brand.backend.application.subscription.service.SubscriptionService;
import com.brand.backend.application.user.service.UserService;
import com.brand.backend.domain.subscription.model.PurchasePlatform;
import com.brand.backend.domain.subscription.model.Subscription;
import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.infrastructure.integration.telegram.user.model.UserSession;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import com.brand.backend.infrastructure.integration.telegram.user.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Обработчик для команд, связанных с подписками.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionHandler {

    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final UserRepository userRepository;
    
    @Qualifier("telegramUserSessionService")
    private final UserSessionService sessionService;
    
    private final TelegramBotService botService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String DESKTOP_APP_DOWNLOAD_LINK = "https://your-cdn.com/downloads/desktop-app/latest";
    
    /**
     * Обрабатывает команду /subscription
     * @param update объект обновления Telegram
     * @return объект сообщения для отправки
     */
    public SendMessage handleSubscriptionCommand(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        
        Optional<User> userOptional = userRepository.findByTelegramId(chatId);
        if (userOptional.isEmpty()) {
            return createMessageWithText(chatId, "Вы не зарегистрированы. Пожалуйста, сначала выполните команду /start");
        }
        
        User user = userOptional.get();
        List<Subscription> activeSubscriptions = subscriptionService.getUserActiveSubscriptions(user.getId());
        
        if (activeSubscriptions.isEmpty()) {
            return createSubscriptionSelectionMenu(chatId);
        } else {
            return createActiveSubscriptionsMessage(chatId, activeSubscriptions);
        }
    }
    
    /**
     * Обрабатывает команду /activate
     * @param update объект обновления Telegram
     * @return объект сообщения для отправки
     */
    public SendMessage handleActivateCommand(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        
        Optional<User> userOptional = userRepository.findByTelegramId(chatId);
        if (userOptional.isEmpty()) {
            return createMessageWithText(chatId, "Вы не зарегистрированы. Пожалуйста, сначала выполните команду /start");
        }
        
        // Устанавливаем состояние ожидания кода активации
        sessionService.setUserState(chatId.toString(), "waitingForActivationCode");
        
        return createMessageWithText(chatId, """
                Пожалуйста, введите код активации для вашей подписки.
                
                Если вы не знаете свой код активации, вы можете получить его с помощью команды /subscription
                """);
    }
    
    /**
     * Обрабатывает ввод кода активации
     * @param chatId ID чата
     * @param activationCode код активации
     * @return объект сообщения для отправки
     */
    public SendMessage handleActivationCodeInput(Long chatId, String activationCode) {
        try {
            Subscription subscription = subscriptionService.activateSubscription(activationCode);
            
            StringBuilder message = new StringBuilder();
            message.append("✅ Подписка успешно активирована!\n\n")
                   .append("Уровень: *")
                   .append(getSubscriptionLevelName(subscription.getSubscriptionLevel()))
                   .append("*\n")
                   .append("Действует до: *")
                   .append(subscription.getEndDate().format(DATE_FORMATTER))
                   .append("*\n\n")
                   .append("Скачать приложение можно по ссылке ниже:");
            
            SendMessage sendMessage = createMessageWithText(chatId, message.toString());
            
            // Добавляем кнопку для скачивания
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton downloadButton = new InlineKeyboardButton();
            downloadButton.setText("Скачать приложение");
            downloadButton.setUrl(DESKTOP_APP_DOWNLOAD_LINK);
            row.add(downloadButton);
            keyboard.add(row);
            
            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);
            
            // Сбрасываем состояние
            sessionService.clearSession(chatId.toString());
            
            return sendMessage;
        } catch (Exception e) {
            return createMessageWithText(chatId, "❌ Ошибка активации: " + e.getMessage() + 
                    "\n\nПроверьте правильность кода и попробуйте снова, или используйте /subscription для получения нового кода.");
        }
    }
    
    /**
     * Обрабатывает выбор уровня подписки
     * @param chatId ID чата
     * @param level уровень подписки
     * @return объект сообщения для отправки
     */
    public SendMessage handleSubscriptionLevelSelected(Long chatId, SubscriptionLevel level) {
        Optional<User> userOptional = userRepository.findByTelegramId(chatId);
        if (userOptional.isEmpty()) {
            return createMessageWithText(chatId, "Вы не зарегистрированы. Пожалуйста, сначала выполните команду /start");
        }
        
        User user = userOptional.get();
        
        // Здесь в реальном сценарии должна быть обработка платежей
        // Пока создаем подписку напрямую
        int durationInDays = getDurationForLevel(level);
        Subscription subscription = subscriptionService.createSubscription(
                user.getId(),
                level,
                durationInDays,
                PurchasePlatform.TELEGRAM
        );
        
        StringBuilder message = new StringBuilder();
        message.append("Ваша подписка уровня *")
                .append(getSubscriptionLevelName(level))
                .append("* успешно создана!\n\n")
                .append("Код активации: `")
                .append(subscription.getActivationCode())
                .append("`\n\n")
                .append("Сохраните этот код, он понадобится для активации десктоп-приложения.\n\n")
                .append("Для активации подписки и получения ссылки на скачивание приложения, используйте команду /activate");
        
        return createMessageWithText(chatId, message.toString());
    }
    
    /**
     * Обрабатывает команду активации подписки
     * @param chatId ID чата
     * @param activationCode код активации
     * @return объект сообщения для отправки
     */
    public SendMessage handleActivateSubscription(Long chatId, String activationCode) {
        try {
            Subscription subscription = subscriptionService.activateSubscription(activationCode);
            
            StringBuilder message = new StringBuilder();
            message.append("Подписка успешно активирована!\n\n")
                    .append("Уровень: *")
                    .append(getSubscriptionLevelName(subscription.getSubscriptionLevel()))
                    .append("*\n")
                    .append("Действует до: *")
                    .append(subscription.getEndDate().format(DATE_FORMATTER))
                    .append("*\n");
            
            return createMessageWithText(chatId, message.toString());
        } catch (Exception e) {
            return createMessageWithText(chatId, "Ошибка активации: " + e.getMessage());
        }
    }
    
    /**
     * Создает меню выбора уровня подписки
     * @param chatId ID чата
     * @return объект сообщения с меню
     */
    public SendMessage createSubscriptionSelectionMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите уровень подписки для десктоп-приложения:");
        message.enableMarkdown(true);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton standardButton = new InlineKeyboardButton();
        standardButton.setText("Стандартный (30 дней)");
        standardButton.setCallbackData("subscription_select_STANDARD");
        row1.add(standardButton);
        keyboard.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton premiumButton = new InlineKeyboardButton();
        premiumButton.setText("Премиум (90 дней)");
        premiumButton.setCallbackData("subscription_select_PREMIUM");
        row2.add(premiumButton);
        keyboard.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton deluxeButton = new InlineKeyboardButton();
        deluxeButton.setText("Делюкс (365 дней)");
        deluxeButton.setCallbackData("subscription_select_DELUXE");
        row3.add(deluxeButton);
        keyboard.add(row3);
        
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        
        return message;
    }
    
    /**
     * Создает сообщение с информацией об активных подписках
     * @param chatId ID чата
     * @param subscriptions список активных подписок
     * @return объект сообщения с информацией
     */
    private SendMessage createActiveSubscriptionsMessage(Long chatId, List<Subscription> subscriptions) {
        StringBuilder message = new StringBuilder();
        message.append("Ваши активные подписки:\n\n");
        
        for (Subscription subscription : subscriptions) {
            message.append("Уровень: *")
                    .append(getSubscriptionLevelName(subscription.getSubscriptionLevel()))
                    .append("*\n")
                    .append("Код активации: `")
                    .append(subscription.getActivationCode())
                    .append("`\n");
            
            if (subscription.getStartDate() != null) {
                message.append("Начало: *")
                        .append(subscription.getStartDate().format(DATE_FORMATTER))
                        .append("*\n");
            }
            
            if (subscription.getEndDate() != null) {
                message.append("Окончание: *")
                        .append(subscription.getEndDate().format(DATE_FORMATTER))
                        .append("*\n");
            }
            
            message.append("Статус: *")
                    .append(subscription.isActive() ? "Активна" : "Не активирована")
                    .append("*\n\n");
        }
        
        message.append("Для покупки новой подписки используйте кнопки ниже:\n")
               .append("Для активации подписки и получения ссылки на скачивание, используйте команду /activate");
        
        SendMessage sendMessage = createMessageWithText(chatId, message.toString());
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton newSubscriptionButton = new InlineKeyboardButton();
        newSubscriptionButton.setText("Купить новую подписку");
        newSubscriptionButton.setCallbackData("subscription_new");
        row1.add(newSubscriptionButton);
        keyboard.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton activateButton = new InlineKeyboardButton();
        activateButton.setText("Активировать подписку");
        activateButton.setCallbackData("subscription_activate");
        row2.add(activateButton);
        keyboard.add(row2);
        
        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);
        
        return sendMessage;
    }
    
    /**
     * Вспомогательный метод для создания сообщения с текстом
     * @param chatId ID чата
     * @param text текст сообщения
     * @return объект сообщения
     */
    private SendMessage createMessageWithText(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableMarkdown(true);
        return message;
    }
    
    /**
     * Возвращает название уровня подписки на русском языке
     * @param level уровень подписки
     * @return название уровня подписки
     */
    private String getSubscriptionLevelName(SubscriptionLevel level) {
        return switch (level) {
            case STANDARD -> "Стандартный";
            case PREMIUM -> "Премиум";
            case DELUXE -> "Делюкс";
        };
    }
    
    /**
     * Определяет длительность подписки в днях в зависимости от уровня
     * @param level уровень подписки
     * @return длительность подписки в днях
     */
    private int getDurationForLevel(SubscriptionLevel level) {
        return switch (level) {
            case STANDARD -> 30;  // 1 месяц
            case PREMIUM -> 90;   // 3 месяца
            case DELUXE -> 365;   // 1 год
        };
    }
} 