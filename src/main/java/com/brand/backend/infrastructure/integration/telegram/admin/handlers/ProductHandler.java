package com.brand.backend.infrastructure.integration.telegram.admin.handlers;

import com.brand.backend.infrastructure.integration.telegram.admin.keyboards.AdminKeyboards;
import com.brand.backend.infrastructure.integration.telegram.admin.service.AdminBotService;
import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.product.repository.ProductRepository;
import com.brand.backend.application.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductHandler {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final AdminBotService adminBotService;
    
    // Регулярное выражение для парсинга ввода цены товара: 1000.50
    private static final Pattern PRICE_PATTERN = Pattern.compile("^(\\d+(\\.\\d+)?)$");
    
    // Регулярное выражение для парсинга ввода запасов товара: 10 20 30
    private static final Pattern STOCK_PATTERN = Pattern.compile("^(\\d+)\\s+(\\d+)\\s+(\\d+)$");
    
    // Регулярное выражение для создания товара: название 1000.50 10 20 30
    private static final Pattern CREATE_PRODUCT_PATTERN = Pattern.compile("^([^\\d]+)\\s+(\\d+(\\.\\d+)?)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)$");
    
    /**
     * Обрабатывает команду отображения всех товаров
     */
    public SendMessage handleAllProducts(String chatId) {
        List<Product> products = productRepository.findAll();
        
        if (products.isEmpty()) {
            return createMessage(chatId, "*👕 Товары*\n\nНет доступных товаров.", AdminKeyboards.createProductsKeyboard());
        }
        
        StringBuilder message = new StringBuilder("*👕 Все товары*\n\n");
        
        for (Product product : products) {
            message.append(formatProductShort(product)).append("\n\n");
        }
        
        return createMessage(chatId, message.toString(), AdminKeyboards.createProductsKeyboard());
    }
    
    /**
     * Обрабатывает команду поиска товара
     */
    public SendMessage handleProductSearchRequest(String chatId) {
        String text = """
                *🔍 Поиск товара*
                
                Введите часть названия товара для поиска.
                
                Например: `/product_search футболка`
                """;
        
        return createMessage(chatId, text, AdminKeyboards.createBackKeyboard("product:all"));
    }
    
    /**
     * Обрабатывает поиск товара
     */
    public SendMessage handleProductSearch(String chatId, String query) {
        List<Product> products = productRepository.findAll()
                .stream()
                .filter(p -> p.getName().toLowerCase().contains(query.toLowerCase()))
                .toList();
        
        if (products.isEmpty()) {
            return createMessage(chatId, "*🔍 Поиск товаров*\n\nТовары по запросу \"" + query + "\" не найдены.", 
                    AdminKeyboards.createBackKeyboard("product:all"));
        }
        
        StringBuilder message = new StringBuilder("*🔍 Результаты поиска*\n\n");
        message.append("Найдено товаров: ").append(products.size()).append("\n\n");
        
        for (Product product : products) {
            message.append(formatProductShort(product)).append("\n\n");
        }
        
        return createMessage(chatId, message.toString(), AdminKeyboards.createBackKeyboard("product:all"));
    }
    
    /**
     * Обрабатывает отображение деталей товара
     */
    public SendMessage handleProductDetails(String chatId, Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        
        if (productOpt.isEmpty()) {
            return createMessage(chatId, "❌ Товар не найден.", AdminKeyboards.createBackKeyboard("product:all"));
        }
        
        Product product = productOpt.get();
        
        return createMessage(chatId, 
                "*👕 Детали товара*\n\n" + formatProductFull(product), 
                AdminKeyboards.createProductDetailsKeyboard(product.getId()));
    }
    
    /**
     * Обрабатывает запрос изменения цены товара
     */
    public SendMessage handleUpdatePriceRequest(String chatId, Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        
        if (productOpt.isEmpty()) {
            return createMessage(chatId, "❌ Товар не найден.", AdminKeyboards.createBackKeyboard("product:all"));
        }
        
        Product product = productOpt.get();
        
        String text = String.format("""
                *💰 Изменение цены товара*
                
                Текущее название: *%s*
                Текущая цена: *%.2f*
                
                Введите новую цену в формате:
                `/product_price_%d 1000.50`
                """, product.getName(), product.getPrice(), product.getId());
        
        return createMessage(chatId, text, AdminKeyboards.createBackKeyboard("product:details:" + productId));
    }
    
    /**
     * Обрабатывает изменение цены товара
     */
    public SendMessage handleUpdatePrice(String chatId, Long productId, String priceText) {
        Optional<Product> productOpt = productRepository.findById(productId);
        
        if (productOpt.isEmpty()) {
            return createMessage(chatId, "❌ Товар не найден.", AdminKeyboards.createBackKeyboard("product:all"));
        }
        
        Matcher matcher = PRICE_PATTERN.matcher(priceText);
        
        if (!matcher.matches()) {
            return createMessage(chatId, "❌ Неверный формат цены. Используйте формат: 1000.50", 
                    AdminKeyboards.createBackKeyboard("product:details:" + productId));
        }
        
        double newPrice = Double.parseDouble(matcher.group(1));
        Product product = productOpt.get();
        double oldPrice = product.getPrice();
        
        product.setPrice(newPrice);
        productRepository.save(product);
        
        String text = String.format("""
                ✅ Цена товара изменена!
                
                *%s*
                
                Старая цена: *%.2f*
                Новая цена: *%.2f*
                """, product.getName(), oldPrice, newPrice);
        
        return createMessage(chatId, text, AdminKeyboards.createProductDetailsKeyboard(productId));
    }
    
    /**
     * Обрабатывает запрос обновления запасов товара
     */
    public SendMessage handleUpdateStockRequest(String chatId, Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        
        if (productOpt.isEmpty()) {
            return createMessage(chatId, "❌ Товар не найден.", AdminKeyboards.createBackKeyboard("product:all"));
        }
        
        Product product = productOpt.get();
        
        String text = String.format("""
                *📦 Обновление запасов товара*
                
                Текущее название: *%s*
                
                Текущие запасы:
                S: *%d* шт.
                M: *%d* шт.
                L: *%d* шт.
                
                Введите новые запасы в формате:
                `/product_stock_%d 10 20 30`
                
                где числа - количество товаров размеров S, M и L соответственно.
                """, product.getName(), 
                product.getAvailableQuantityS(), 
                product.getAvailableQuantityM(), 
                product.getAvailableQuantityL(),
                product.getId());
        
        return createMessage(chatId, text, AdminKeyboards.createBackKeyboard("product:details:" + productId));
    }
    
    /**
     * Обрабатывает обновление запасов товара
     */
    public SendMessage handleUpdateStock(String chatId, Long productId, String stockText) {
        Optional<Product> productOpt = productRepository.findById(productId);
        
        if (productOpt.isEmpty()) {
            return createMessage(chatId, "❌ Товар не найден.", AdminKeyboards.createBackKeyboard("product:all"));
        }
        
        Matcher matcher = STOCK_PATTERN.matcher(stockText);
        
        if (!matcher.matches()) {
            return createMessage(chatId, "❌ Неверный формат ввода запасов. Используйте формат: 10 20 30", 
                    AdminKeyboards.createBackKeyboard("product:details:" + productId));
        }
        
        int quantityS = Integer.parseInt(matcher.group(1));
        int quantityM = Integer.parseInt(matcher.group(2));
        int quantityL = Integer.parseInt(matcher.group(3));
        
        Product product = productOpt.get();
        int oldS = product.getAvailableQuantityS();
        int oldM = product.getAvailableQuantityM();
        int oldL = product.getAvailableQuantityL();
        
        product.setAvailableQuantityS(quantityS);
        product.setAvailableQuantityM(quantityM);
        product.setAvailableQuantityL(quantityL);
        productRepository.save(product);
        
        String text = String.format("""
                ✅ Запасы товара обновлены!
                
                *%s*
                
                Старые запасы:
                S: *%d* шт.
                M: *%d* шт.
                L: *%d* шт.
                
                Новые запасы:
                S: *%d* шт.
                M: *%d* шт.
                L: *%d* шт.
                """, product.getName(), oldS, oldM, oldL, quantityS, quantityM, quantityL);
        
        return createMessage(chatId, text, AdminKeyboards.createProductDetailsKeyboard(productId));
    }
    
    /**
     * Обрабатывает запрос на создание нового товара
     */
    public SendMessage handleCreateProductRequest(String chatId) {
        String text = """
                *➕ Создание нового товара*
                
                Введите данные товара в формате:
                `/product_create Название товара 1000.50 10 20 30`
                
                где:
                - Название товара - название нового товара
                - 1000.50 - цена товара
                - 10 - количество размера S
                - 20 - количество размера M
                - 30 - количество размера L
                """;
        
        return createMessage(chatId, text, AdminKeyboards.createBackKeyboard("product:all"));
    }
    
    /**
     * Обрабатывает создание нового товара
     */
    public SendMessage handleCreateProduct(String chatId, String productData) {
        Matcher matcher = CREATE_PRODUCT_PATTERN.matcher(productData);
        
        if (!matcher.matches()) {
            return createMessage(chatId, "❌ Неверный формат данных товара. Используйте формат: Название товара 1000.50 10 20 30", 
                    AdminKeyboards.createBackKeyboard("product:all"));
        }
        
        String name = matcher.group(1).trim();
        double price = Double.parseDouble(matcher.group(2));
        int quantityS = Integer.parseInt(matcher.group(4));
        int quantityM = Integer.parseInt(matcher.group(5));
        int quantityL = Integer.parseInt(matcher.group(6));
        
        Product product = productService.createProduct(name, price, quantityS, quantityM, quantityL);
        
        String text = String.format("""
                ✅ Товар успешно создан!
                
                *%s*
                
                Цена: *%.2f*
                
                Запасы:
                S: *%d* шт.
                M: *%d* шт.
                L: *%d* шт.
                """, product.getName(), product.getPrice(), 
                product.getAvailableQuantityS(), 
                product.getAvailableQuantityM(), 
                product.getAvailableQuantityL());
        
        return createMessage(chatId, text, AdminKeyboards.createProductDetailsKeyboard(product.getId()));
    }
    
    /**
     * Форматирует товар для краткого отображения
     */
    private String formatProductShort(Product product) {
        StringBuilder result = new StringBuilder();
        
        // Экранируем название товара от спецсимволов Markdown
        String escapedName = escapeMarkdown(product.getName());
        result.append("*").append(escapedName).append("*\n");
        result.append("💰 Цена: ").append(String.format("%.2f", product.getPrice())).append("\n");
        
        int totalQuantity = product.getAvailableQuantityS() + product.getAvailableQuantityM() + product.getAvailableQuantityL();
        result.append("📦 В наличии: ").append(totalQuantity).append(" шт.\n");
        
        result.append("👁 /product\\_").append(product.getId());
        
        return result.toString();
    }
    
    /**
     * Форматирует товар для подробного отображения
     */
    private String formatProductFull(Product product) {
        StringBuilder result = new StringBuilder();
        
        // Экранируем название товара от спецсимволов Markdown
        String escapedName = escapeMarkdown(product.getName());
        result.append("*Название:* ").append(escapedName).append("\n");
        result.append("*Цена:* ").append(String.format("%.2f", product.getPrice())).append("\n\n");
        
        result.append("*Доступные размеры:* ");
        if (!product.getSizes().isEmpty()) {
            // Экранируем каждый размер
            result.append(String.join(", ", 
                product.getSizes().stream()
                    .map(this::escapeMarkdown)
                    .toList()));
        } else {
            result.append("-");
        }
        result.append("\n\n");
        
        result.append("*Количество в наличии:*\n");
        result.append("S: ").append(product.getAvailableQuantityS()).append(" шт.\n");
        result.append("M: ").append(product.getAvailableQuantityM()).append(" шт.\n");
        result.append("L: ").append(product.getAvailableQuantityL()).append(" шт.\n\n");
        
        int totalQuantity = product.getAvailableQuantityS() + product.getAvailableQuantityM() + product.getAvailableQuantityL();
        result.append("*Всего в наличии:* ").append(totalQuantity).append(" шт.\n");
        
        return result.toString();
    }
    
    /**
     * Экранирует специальные символы Markdown
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        // Экранируем специальные символы Markdown: * _ ` [ ] ( )
        return text.replace("*", "\\*")
                  .replace("_", "\\_")
                  .replace("`", "\\`")
                  .replace("[", "\\[")
                  .replace("]", "\\]")
                  .replace("(", "\\(")
                  .replace(")", "\\)")
                  .replace(".", "\\.")
                  .replace("!", "\\!")
                  .replace("-", "\\-")
                  .replace("+", "\\+")
                  .replace("=", "\\=")
                  .replace("#", "\\#")
                  .replace("~", "\\~")
                  .replace("|", "\\|")
                  .replace(">", "\\>")
                  .replace("<", "\\<");
    }
    
    /**
     * Создаёт объект сообщения
     */
    private SendMessage createMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        return message;
    }
    
    /**
     * Создаёт объект сообщения с клавиатурой
     */
    private SendMessage createMessage(String chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard) {
        SendMessage message = createMessage(chatId, text);
        message.setReplyMarkup(keyboard);
        return message;
    }
    
    /**
     * Обрабатывает запрос на удаление товара
     */
    public SendMessage handleDeleteProductRequest(String chatId, Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        
        if (productOpt.isEmpty()) {
            return createMessage(chatId, "❌ Товар не найден.", AdminKeyboards.createBackKeyboard("product:all"));
        }
        
        Product product = productOpt.get();
        
        String text = String.format("""
                *🗑️ Удаление товара*
                
                Вы действительно хотите удалить товар?
                
                *%s*
                
                Цена: *%.2f*
                
                Запасы:
                S: *%d* шт.
                M: *%d* шт.
                L: *%d* шт.
                """, product.getName(), product.getPrice(),
                product.getAvailableQuantityS(),
                product.getAvailableQuantityM(),
                product.getAvailableQuantityL());
        
        return createMessage(chatId, text, AdminKeyboards.createConfirmKeyboard("product:delete", productId.toString()));
    }
    
    /**
     * Обрабатывает удаление товара
     */
    public SendMessage handleDeleteProduct(String chatId, Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        
        if (productOpt.isEmpty()) {
            return createMessage(chatId, "❌ Товар не найден.", AdminKeyboards.createBackKeyboard("product:all"));
        }
        
        Product product = productOpt.get();
        String productName = product.getName();
        
        try {
            productService.deleteProduct(productId);
            
            return createMessage(chatId, String.format("""
                    ✅ Товар *%s* успешно удален!
                    """, escapeMarkdown(productName)), AdminKeyboards.createBackKeyboard("product:all"));
        } catch (Exception e) {
            log.error("Ошибка при удалении товара: {}", e.getMessage());
            return createMessage(chatId, "❌ Ошибка при удалении товара.", AdminKeyboards.createBackKeyboard("product:details:" + productId));
        }
    }
} 