package com.brand.backend.infrastructure.integration.telegram.user.command;

import com.brand.backend.infrastructure.integration.telegram.user.model.CartItem;
import com.brand.backend.infrastructure.integration.telegram.user.service.CartService;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда для работы с корзиной покупок
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CartCommand implements Command {

    private final CartService cartService;
    private final TelegramBotService botService;

    @Override
    public String getCommandName() {
        return "/cart";
    }

    @Override
    public void execute(Message message, TelegramBotService bot) {
        Long chatId = message.getChatId();
        showCart(chatId);
    }
    
    /**
     * Отображает содержимое корзины пользователя
     * 
     * @param chatId ID чата пользователя
     */
    public void showCart(Long chatId) {
        List<CartItem> items = cartService.getCartItems(chatId);
        
        if (items.isEmpty()) {
            String emptyCartMessage = """
                    🛒 *Ваша корзина пуста*
                    
                    Добавьте товары в корзину, чтобы оформить заказ.
                    
                    Используйте команду /shop для просмотра каталога товаров.
                    """;
            botService.sendMessage(chatId.toString(), emptyCartMessage);
            return;
        }
        
        double total = cartService.calculateTotal(chatId);
        
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("🛒 *Ваша корзина:*\n\n");
        
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            messageBuilder.append(String.format("%d. %s\n", (i + 1), item));
        }
        
        messageBuilder.append("\n💵 *Общая стоимость: %.2f руб.*\n\n");
        messageBuilder.append("Выберите действие из меню ниже:");
        
        String message = String.format(messageBuilder.toString(), total);
        
        botService.sendMessage(chatId.toString(), message, createCartKeyboard(items));
    }
    
    /**
     * Создает клавиатуру для действий с корзиной
     * 
     * @param items список товаров в корзине
     * @return клавиатура действий
     */
    private InlineKeyboardMarkup createCartKeyboard(List<CartItem> items) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        
        // Ряды с кнопками для каждого товара
        for (int i = 0; i < items.size(); i++) {
            List<InlineKeyboardButton> itemRow = new ArrayList<>();
            
            // Кнопка уменьшения количества
            InlineKeyboardButton decreaseButton = new InlineKeyboardButton();
            decreaseButton.setText("➖");
            decreaseButton.setCallbackData("cart_decrease_" + i);
            
            // Кнопка увеличения количества
            InlineKeyboardButton increaseButton = new InlineKeyboardButton();
            increaseButton.setText("➕");
            increaseButton.setCallbackData("cart_increase_" + i);
            
            // Кнопка удаления товара
            InlineKeyboardButton removeButton = new InlineKeyboardButton();
            removeButton.setText("🗑");
            removeButton.setCallbackData("cart_remove_" + i);
            
            itemRow.add(decreaseButton);
            itemRow.add(increaseButton);
            itemRow.add(removeButton);
            
            rowList.add(itemRow);
        }
        
        // Ряд с кнопками оформления заказа и очистки корзины
        List<InlineKeyboardButton> actionRow = new ArrayList<>();
        
        InlineKeyboardButton checkoutButton = new InlineKeyboardButton();
        checkoutButton.setText("✅ Оформить заказ");
        checkoutButton.setCallbackData("checkout");
        
        InlineKeyboardButton clearButton = new InlineKeyboardButton();
        clearButton.setText("🗑️ Очистить корзину");
        clearButton.setCallbackData("clear_cart");
        
        actionRow.add(checkoutButton);
        actionRow.add(clearButton);
        rowList.add(actionRow);
        
        // Ряд с кнопкой возврата к покупкам
        List<InlineKeyboardButton> shopRow = new ArrayList<>();
        
        InlineKeyboardButton shopButton = new InlineKeyboardButton();
        shopButton.setText("🔙 Вернуться к покупкам");
        shopButton.setCallbackData("shop");
        
        shopRow.add(shopButton);
        rowList.add(shopRow);
        
        markup.setKeyboard(rowList);
        return markup;
    }
    
    /**
     * Обрабатывает очистку корзины
     * 
     * @param chatId ID чата пользователя
     */
    public void handleClearCart(Long chatId) {
        cartService.clearCart(chatId);
        
        String message = """
                🗑️ *Корзина очищена*
                
                Ваша корзина была очищена. 
                
                Используйте команду /shop для просмотра каталога товаров.
                """;
        
        botService.sendMessage(chatId.toString(), message);
    }
    
    /**
     * Обрабатывает удаление товара из корзины
     * 
     * @param chatId ID чата пользователя
     * @param itemIndex индекс товара
     */
    public void handleRemoveItem(Long chatId, int itemIndex) {
        boolean removed = cartService.removeFromCart(chatId, itemIndex);
        
        if (removed) {
            String message = "✅ Товар удален из корзины.";
            botService.sendMessage(chatId.toString(), message);
            showCart(chatId);
        } else {
            String message = "❌ Не удалось удалить товар. Попробуйте еще раз.";
            botService.sendMessage(chatId.toString(), message);
        }
    }
    
    /**
     * Обрабатывает увеличение количества товара на 1
     * 
     * @param chatId ID чата пользователя
     * @param itemIndex индекс товара
     */
    public void handleIncreaseQuantity(Long chatId, int itemIndex) {
        List<CartItem> items = cartService.getCartItems(chatId);
        if (itemIndex >= 0 && itemIndex < items.size()) {
            CartItem item = items.get(itemIndex);
            int newQuantity = item.getQuantity() + 1;
            handleUpdateQuantity(chatId, itemIndex, newQuantity);
        } else {
            String message = "❌ Товар не найден. Попробуйте еще раз.";
            botService.sendMessage(chatId.toString(), message);
        }
    }
    
    /**
     * Обрабатывает уменьшение количества товара на 1
     * 
     * @param chatId ID чата пользователя
     * @param itemIndex индекс товара
     */
    public void handleDecreaseQuantity(Long chatId, int itemIndex) {
        List<CartItem> items = cartService.getCartItems(chatId);
        if (itemIndex >= 0 && itemIndex < items.size()) {
            CartItem item = items.get(itemIndex);
            int newQuantity = item.getQuantity() - 1;
            // Если количество становится меньше 1, удаляем товар
            if (newQuantity < 1) {
                handleRemoveItem(chatId, itemIndex);
            } else {
                handleUpdateQuantity(chatId, itemIndex, newQuantity);
            }
        } else {
            String message = "❌ Товар не найден. Попробуйте еще раз.";
            botService.sendMessage(chatId.toString(), message);
        }
    }
    
    /**
     * Обрабатывает изменение количества товара
     * 
     * @param chatId ID чата пользователя
     * @param itemIndex индекс товара
     * @param newQuantity новое количество
     */
    public void handleUpdateQuantity(Long chatId, int itemIndex, int newQuantity) {
        boolean updated = cartService.updateQuantity(chatId, itemIndex, newQuantity);
        
        if (updated) {
            String message = "✅ Количество товара обновлено.";
            botService.sendMessage(chatId.toString(), message);
            showCart(chatId);
        } else {
            String message = "❌ Не удалось обновить количество товара. Попробуйте еще раз.";
            botService.sendMessage(chatId.toString(), message);
        }
    }
} 