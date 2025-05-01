package com.brand.backend.infrastructure.integration.telegram.admin.handlers;

import com.brand.backend.infrastructure.integration.telegram.admin.keyboards.AdminKeyboards;
import com.brand.backend.infrastructure.integration.telegram.admin.service.AdminBotService;
import com.brand.backend.domain.promotion.model.PromoCode;
import com.brand.backend.application.promotion.service.PromoCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromoCodeHandler {

    private final PromoCodeService promoCodeService;
    private final AdminBotService adminBotService;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    
    // Регулярное выражение для парсинга ввода промокода в формате: ПРОМО20 30% 100 Описание промокода
    private static final Pattern CREATE_PATTERN = Pattern.compile("^([A-Za-z0-9]+)\\s+(\\d+)%\\s+(\\d+)(?:\\s+(.+))?$");
    
    /**
     * Обрабатывает команду отображения всех промокодов
     */
    public SendMessage handleAllPromoCodes(String chatId) {
        List<PromoCode> promoCodes = promoCodeService.getAllPromoCodes();
        
        if (promoCodes.isEmpty()) {
            return createMessage(chatId, "*🔖 Промокоды*\n\nНет доступных промокодов.", AdminKeyboards.createPromoCodesKeyboard());
        }
        
        StringBuilder message = new StringBuilder("*🔖 Все промокоды*\n\n");
        
        for (PromoCode promoCode : promoCodes) {
            message.append(formatPromoCodeShort(promoCode)).append("\n\n");
        }
        
        return createMessage(chatId, message.toString(), AdminKeyboards.createPromoCodesKeyboard());
    }
    
    /**
     * Обрабатывает команду отображения активных промокодов
     */
    public SendMessage handleActivePromoCodes(String chatId) {
        List<PromoCode> promoCodes = promoCodeService.getActivePromoCodes();
        
        if (promoCodes.isEmpty()) {
            return createMessage(chatId, "*🔖 Активные промокоды*\n\nНет активных промокодов.", AdminKeyboards.createPromoCodesKeyboard());
        }
        
        StringBuilder message = new StringBuilder("*🔖 Активные промокоды*\n\n");
        
        for (PromoCode promoCode : promoCodes) {
            message.append(formatPromoCodeShort(promoCode)).append("\n\n");
        }
        
        return createMessage(chatId, message.toString(), AdminKeyboards.createPromoCodesKeyboard());
    }
    
    /**
     * Обрабатывает запрос создания промокода
     */
    public SendMessage handleCreatePromoCodeRequest(String chatId) {
        String text = """
                *➕ Создание промокода*
                
                Отправьте данные в формате:
                `/promo_create КОД СКИДКА% МАКС_ИСПОЛЬЗОВАНИЙ [ОПИСАНИЕ]`
                
                Например:
                `/promo_create SUMMER2023 15% 100 Летняя скидка`
                
                Промокод будет активен сразу после создания.
                """;
        
        return createMessage(chatId, text, AdminKeyboards.createBackKeyboard("promo:all"));
    }
    
    /**
     * Обрабатывает создание промокода
     */
    public SendMessage handleCreatePromoCode(String chatId, String text) {
        log.info("Получен запрос на создание промокода: {}", text);
        
        Matcher matcher = CREATE_PATTERN.matcher(text);
        
        if (!matcher.matches()) {
            log.warn("Неверный формат запроса для создания промокода: {}", text);
            return createMessage(chatId, "❌ Неверный формат ввода. Используйте:\n`КОД СКИДКА% МАКС_ИСПОЛЬЗОВАНИЙ [ОПИСАНИЕ]`");
        }
        
        String code = matcher.group(1).toUpperCase();
        int discountPercent = Integer.parseInt(matcher.group(2));
        int maxUses = Integer.parseInt(matcher.group(3));
        String description = matcher.group(4) != null ? matcher.group(4) : "";
        
        log.info("Создание промокода с параметрами: код={}, скидка={}%, макс.использований={}, описание='{}'",
                code, discountPercent, maxUses, description);
        
        try {
            PromoCode promoCode = promoCodeService.createPromoCode(
                    code, 
                    discountPercent, 
                    maxUses, 
                    LocalDateTime.now(), 
                    null, // Без ограничения по времени
                    description
            );
            
            log.info("Промокод успешно создан с ID: {}", promoCode.getId());
            
            return createMessage(chatId, 
                    "✅ Промокод создан успешно!\n\n" + formatPromoCodeFull(promoCode), 
                    AdminKeyboards.createPromoCodeDetailsKeyboard(promoCode.getId(), promoCode.isActive()));
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при создании промокода: {}", e.getMessage());
            return createMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает отображение деталей промокода
     */
    public SendMessage handlePromoCodeDetails(String chatId, Long promoId) {
        try {
            PromoCode promoCode = promoCodeService.getPromoCodeById(promoId)
                    .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
            
            return createMessage(chatId, 
                    "*🔖 Детали промокода*\n\n" + formatPromoCodeFull(promoCode), 
                    AdminKeyboards.createPromoCodeDetailsKeyboard(promoCode.getId(), promoCode.isActive()));
        } catch (IllegalArgumentException e) {
            return createMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает активацию промокода
     */
    public EditMessageText handleActivatePromoCode(String chatId, Integer messageId, Long promoId) {
        try {
            PromoCode promoCode = promoCodeService.activatePromoCode(promoId);
            
            return EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("✅ Промокод активирован!\n\n" + formatPromoCodeFull(promoCode))
                    .parseMode("Markdown")
                    .replyMarkup((InlineKeyboardMarkup) AdminKeyboards.createPromoCodeDetailsKeyboard(promoCode.getId(), promoCode.isActive()))
                    .build();
        } catch (IllegalArgumentException e) {
            return EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("❌ Ошибка: " + e.getMessage())
                    .parseMode("Markdown")
                    .build();
        }
    }
    
    /**
     * Обрабатывает деактивацию промокода
     */
    public EditMessageText handleDeactivatePromoCode(String chatId, Integer messageId, Long promoId) {
        try {
            PromoCode promoCode = promoCodeService.deactivatePromoCode(promoId);
            
            return EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("🚫 Промокод деактивирован!\n\n" + formatPromoCodeFull(promoCode))
                    .parseMode("Markdown")
                    .replyMarkup((InlineKeyboardMarkup) AdminKeyboards.createPromoCodeDetailsKeyboard(promoCode.getId(), promoCode.isActive()))
                    .build();
        } catch (IllegalArgumentException e) {
            return EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("❌ Ошибка: " + e.getMessage())
                    .parseMode("Markdown")
                    .build();
        }
    }
    
    /**
     * Обрабатывает удаление промокода
     */
    public EditMessageText handleDeletePromoCode(String chatId, Integer messageId, Long promoId) {
        try {
            PromoCode promoCode = promoCodeService.getPromoCodeById(promoId)
                    .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
            
            String promoText = formatPromoCodeFull(promoCode);
            promoCodeService.deletePromoCode(promoId);
            
            return EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("🗑️ Промокод удален!\n\n" + promoText)
                    .parseMode("Markdown")
                    .replyMarkup((InlineKeyboardMarkup) AdminKeyboards.createBackKeyboard("promo:all"))
                    .build();
        } catch (IllegalArgumentException e) {
            return EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("❌ Ошибка: " + e.getMessage())
                    .parseMode("Markdown")
                    .build();
        }
    }
    
    /**
     * Форматирует промокод для краткого отображения
     */
    private String formatPromoCodeShort(PromoCode promoCode) {
        StringBuilder result = new StringBuilder();
        
        result.append("*").append(promoCode.getCode()).append("* - ");
        result.append(promoCode.getDiscountPercent()).append("%\n");
        
        result.append(promoCode.isActive() ? "✅ Активен" : "❌ Неактивен");
        result.append(" | Использован: ").append(promoCode.getUsedCount()).append("/").append(promoCode.getMaxUses());
        
        result.append("\n👁 /promo\\_").append(promoCode.getId());
        
        return result.toString();
    }
    
    /**
     * Форматирует промокод для подробного отображения
     */
    private String formatPromoCodeFull(PromoCode promoCode) {
        StringBuilder result = new StringBuilder();
        
        result.append("*Код:* `").append(promoCode.getCode()).append("`\n");
        result.append("*Скидка:* ").append(promoCode.getDiscountPercent()).append("%\n");
        result.append("*Статус:* ").append(promoCode.isActive() ? "✅ Активен" : "❌ Неактивен").append("\n");
        result.append("*Использований:* ").append(promoCode.getUsedCount()).append("/").append(promoCode.getMaxUses()).append("\n");
        
        if (promoCode.getStartDate() != null) {
            result.append("*Действует с:* ").append(promoCode.getStartDate().format(formatter)).append("\n");
        }
        
        if (promoCode.getEndDate() != null) {
            result.append("*Действует до:* ").append(promoCode.getEndDate().format(formatter)).append("\n");
        }
        
        if (promoCode.getDescription() != null && !promoCode.getDescription().isEmpty()) {
            result.append("*Описание:* ").append(escapeMarkdown(promoCode.getDescription())).append("\n");
        }
        
        result.append("*Создан:* ").append(promoCode.getCreatedAt().format(formatter)).append("\n");
        
        if (promoCode.getUpdatedAt() != null) {
            result.append("*Обновлен:* ").append(promoCode.getUpdatedAt().format(formatter)).append("\n");
        }
        
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
                  .replace("#", "\\#");
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
     * Обрабатывает запрос на редактирование промокода
     */
    public SendMessage handleEditPromoCodeRequest(String chatId, Long promoId) {
        try {
            PromoCode promoCode = promoCodeService.getPromoCodeById(promoId)
                    .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
            
            String text = String.format("""
                    *✏️ Редактирование промокода*
                    
                    Текущие данные:
                    Код: *%s*
                    Скидка: *%d%%*
                    Макс. использований: *%d*
                    Описание: *%s*
                    
                    Отправьте обновленные данные в формате:
                    `/promo_edit_%d КОД СКИДКА%% МАКС_ИСПОЛЬЗОВАНИЙ [ОПИСАНИЕ]`
                    
                    Например:
                    `/promo_edit_%d WINTER2023 25%% 200 Зимняя скидка`
                    """, 
                    promoCode.getCode(),
                    promoCode.getDiscountPercent(),
                    promoCode.getMaxUses(),
                    promoCode.getDescription() != null ? promoCode.getDescription() : "",
                    promoId,
                    promoId);
            
            return createMessage(chatId, text, AdminKeyboards.createBackKeyboard("promo:details:" + promoId));
        } catch (IllegalArgumentException e) {
            return createMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает обновление данных промокода
     */
    public SendMessage handleUpdatePromoCode(String chatId, Long promoId, String code, int discountPercent, int maxUses, String description) {
        log.info("Обновление промокода {}: код={}, скидка={}%, макс.использований={}, описание='{}'",
                promoId, code, discountPercent, maxUses, description);
        
        try {
            // Получаем текущий промокод
            PromoCode promoCode = promoCodeService.getPromoCodeById(promoId)
                    .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
            
            // Сохраняем старые значения для вывода
            String oldCode = promoCode.getCode();
            int oldDiscountPercent = promoCode.getDiscountPercent();
            int oldMaxUses = promoCode.getMaxUses();
            String oldDescription = promoCode.getDescription() != null ? promoCode.getDescription() : "";
            
            // Обновляем промокод
            promoCode.setCode(code);
            promoCode.setDiscountPercent(discountPercent);
            promoCode.setMaxUses(maxUses);
            promoCode.setDescription(description);
            promoCode.setUpdatedAt(LocalDateTime.now());
            
            PromoCode updatedPromoCode = promoCodeService.updatePromoCode(promoCode);
            
            // Формируем сообщение об успешном обновлении
            StringBuilder resultMessage = new StringBuilder("✅ Промокод успешно обновлен!\n\n");
            
            resultMessage.append("*Старые данные:*\n");
            resultMessage.append("Код: `").append(oldCode).append("`\n");
            resultMessage.append("Скидка: ").append(oldDiscountPercent).append("%\n");
            resultMessage.append("Макс. использований: ").append(oldMaxUses).append("\n");
            if (!oldDescription.isEmpty()) {
                resultMessage.append("Описание: ").append(escapeMarkdown(oldDescription)).append("\n");
            }
            
            resultMessage.append("\n*Новые данные:*\n");
            resultMessage.append(formatPromoCodeFull(updatedPromoCode));
            
            return createMessage(chatId, resultMessage.toString(), 
                    AdminKeyboards.createPromoCodeDetailsKeyboard(updatedPromoCode.getId(), updatedPromoCode.isActive()));
            
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при обновлении промокода: {}", e.getMessage());
            return createMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает обновление промокода из текстового сообщения
     */
    public SendMessage handleUpdatePromoCode(String chatId, Long promoId, String text) {
        log.info("Обновление промокода из текста {}: {}", promoId, text);
        
        Matcher matcher = CREATE_PATTERN.matcher(text);
        
        if (!matcher.matches()) {
            log.warn("Неверный формат запроса для обновления промокода: {}", text);
            return createMessage(chatId, "❌ Неверный формат ввода. Используйте:\n`КОД СКИДКА% МАКС_ИСПОЛЬЗОВАНИЙ [ОПИСАНИЕ]`");
        }
        
        // Парсим новые значения
        String newCode = matcher.group(1).toUpperCase();
        int newDiscountPercent = Integer.parseInt(matcher.group(2));
        int newMaxUses = Integer.parseInt(matcher.group(3));
        String newDescription = matcher.group(4) != null ? matcher.group(4) : "";
        
        return handleUpdatePromoCode(chatId, promoId, newCode, newDiscountPercent, newMaxUses, newDescription);
    }

    /**
     * Формирует сообщение со списком промокодов
     */
    private SendMessage createPromoCodesListMessage(String chatId, List<PromoCode> promoCodes, String title) {
        if (promoCodes == null || promoCodes.isEmpty()) {
            return createMessage(chatId, "*" + title + "*\n\nНет промокодов для отображения.");
        }
        StringBuilder message = new StringBuilder();
        message.append("*" + title + "*\n\n");
        for (PromoCode promo : promoCodes) {
            message.append(formatPromoCodeShort(promo)).append("\n\n");
        }
        return createMessage(chatId, message.toString(), AdminKeyboards.createPromoCodesKeyboard());
    }

    /**
     * Обрабатывает запрос на получение списка истекших промокодов
     */
    public SendMessage handleExpiredPromoCodes(String chatId) {
        log.info(">> Обработка запроса на получение списка истекших промокодов");
        try {
            List<PromoCode> promoCodes = promoCodeService.getAllPromoCodes();
            List<PromoCode> expired = promoCodes.stream().filter(p -> !p.isActive()).toList();
            return createPromoCodesListMessage(chatId, expired, "Истекшие промокоды");
        } catch (Exception e) {
            log.error("Ошибка при получении списка истекших промокодов: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        } finally {
            log.info("<< Завершение обработки запроса на получение списка истекших промокодов");
        }
    }

    /**
     * Обрабатывает запрос на создание промокода (запрос формы)
     */
    public SendMessage handleCreatePromoCode(String chatId) {
        return handleCreatePromoCodeRequest(chatId);
    }
} 