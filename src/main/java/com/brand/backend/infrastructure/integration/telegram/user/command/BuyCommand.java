package com.brand.backend.infrastructure.integration.telegram.user.command;

import com.brand.backend.domain.product.model.Product;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramProductService;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда для обработки /buy
 */
@Component
@RequiredArgsConstructor
public class BuyCommand implements Command {

    private final TelegramProductService productService;

    @Override
    public void execute(Message message, TelegramBotService bot) {
        String chatId = String.valueOf(message.getChatId());
        showProductPage(chatId, 0, bot);
    }

    @Override
    public String getCommandName() {
        return "/buy";
    }

    /**
     * Показывает страницу с товаром
     */
    private void showProductPage(String chatId, int pageIndex, TelegramBotService bot) {
        List<Product> products = productService.getAllProducts();
        
        if (products.isEmpty()) {
            bot.sendMessage(chatId, "Извините, товары временно недоступны.");
            return;
        }

        Product product = products.get(pageIndex);
        
        String text = "👕 " + product.getName() + "\n" +
                "💵 Цена: " + product.getPrice() + " RUB";
        
        bot.sendMessage(chatId, text, createProductKeyboard(product, pageIndex, products.size()));
    }

    /**
     * Создает клавиатуру для страницы товара
     */
    private InlineKeyboardMarkup createProductKeyboard(Product product, int pageIndex, int totalProducts) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопки навигации
        if (pageIndex > 0 || pageIndex < totalProducts - 1) {
            List<InlineKeyboardButton> navigationRow = new ArrayList<>();
            
            if (pageIndex > 0) {
                navigationRow.add(createButton("⬅️ Назад", "page_" + (pageIndex - 1)));
            }
            
            if (pageIndex < totalProducts - 1) {
                navigationRow.add(createButton("➡️ Далее", "page_" + (pageIndex + 1)));
            }
            
            rows.add(navigationRow);
        }

        // Кнопки размеров
        List<InlineKeyboardButton> sizeButtons = new ArrayList<>();
        for (String size : product.getSizes()) {
            sizeButtons.add(createButton(size, "size_" + product.getId() + "_" + size));
        }
        rows.add(sizeButtons);
        
        // Кнопка добавления в корзину
        List<InlineKeyboardButton> cartRow = new ArrayList<>();
        cartRow.add(createButton("🛒 Добавить в корзину", "add_to_cart_" + product.getId()));
        rows.add(cartRow);
        
        // Кнопка возврата в главное меню
        List<InlineKeyboardButton> menuRow = new ArrayList<>();
        menuRow.add(createButton("🔙 Назад в меню", "main_menu"));
        rows.add(menuRow);

        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создает кнопку с заданным текстом и callback-данными
     */
    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
} 