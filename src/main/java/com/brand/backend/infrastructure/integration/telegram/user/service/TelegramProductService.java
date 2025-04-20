package com.brand.backend.infrastructure.integration.telegram.user.service;

import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с товарами в Telegram
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramProductService {

    private final ProductRepository productRepository;
    
    /**
     * Возвращает все товары
     * 
     * @return список товаров
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    /**
     * Возвращает товар по ID
     * 
     * @param productId ID товара
     * @return товар или null, если товар не найден
     */
    public Product getProductById(Long productId) {
        return productRepository.findById(productId).orElse(null);
    }
    
    /**
     * Ищет товары по названию
     * 
     * @param query поисковый запрос
     * @return список найденных товаров
     */
    public List<Product> searchProducts(String query) {
        String lowercaseQuery = query.toLowerCase();
        return productRepository.findAll().stream()
                .filter(product -> product.getName().toLowerCase().contains(lowercaseQuery))
                .toList();
    }
    
    /**
     * Обрабатывает выбор товара пользователем
     * 
     * @param chatId ID чата
     * @param productId ID товара
     * @param size размер товара
     * @param bot экземпляр бота
     */
    public void handleProductSelection(String chatId, Long productId, String size, TelegramBotService bot) {
        Optional<Product> productOptional = productRepository.findById(productId);
        
        if (productOptional.isEmpty()) {
            bot.sendMessage(chatId, "Товар не найден. Попробуйте выбрать другой товар.");
            return;
        }
        
        Product product = productOptional.get();
        
        String message = """
                ✅ *Вы выбрали:*
                
                👕 %s
                📏 Размер: %s
                💵 Цена: %.2f RUB
                
                Добавьте товар в корзину или продолжите покупки.
                """.formatted(product.getName(), size, product.getPrice());
        
        bot.sendMessage(chatId, message, createProductActionKeyboard(productId, size));
    }
    
    /**
     * Создает клавиатуру действий с товаром
     * 
     * @param productId ID товара
     * @param size размер товара
     * @return клавиатура с кнопками
     */
    private org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup createProductActionKeyboard(Long productId, String size) {
        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup markup = new org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup();
        
        List<List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Кнопка добавления в корзину
        List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("🛒 Добавить в корзину", "add_to_cart_" + productId + "_" + size));
        rows.add(row1);
        
        // Кнопка продолжения покупок
        List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🔙 Вернуться к покупкам", "shop"));
        rows.add(row2);
        
        markup.setKeyboard(rows);
        return markup;
    }
    
    /**
     * Создает кнопку с заданным текстом и callback-данными
     */
    private org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton createButton(String text, String callbackData) {
        org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton button = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
} 