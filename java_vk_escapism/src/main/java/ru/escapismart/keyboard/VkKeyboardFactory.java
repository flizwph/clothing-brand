package ru.escapismart.keyboard;

import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonColor;
import com.vk.api.sdk.objects.messages.KeyboardButtonAction;
import com.vk.api.sdk.objects.messages.TemplateActionTypeNames;

import java.util.ArrayList;
import java.util.List;

/**
 * Фабрика для создания клавиатур ВКонтакте.
 * Предоставляет методы для создания различных типов клавиатур,
 * используемых в боте.
 */
public class VkKeyboardFactory {
    
    /**
     * Создает главную клавиатуру с основными действиями.
     */
    public static Keyboard getMainKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        // Кнопки для заказов
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("🛒 Заказать товар"))
                .setColor(KeyboardButtonColor.PRIMARY));
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("📦 Мои заказы"))
                .setColor(KeyboardButtonColor.POSITIVE));
        
        // Кнопки для профиля и дополнительных функций
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("👤 Мой профиль"))
                .setColor(KeyboardButtonColor.DEFAULT));
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("📊 Криптовалюты"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        // Кнопки для чата и помощи
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("💬 Связаться с админом"))
                .setColor(KeyboardButtonColor.DEFAULT));
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("❓ Помощь"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(false);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру управления заказом.
     */
    public static Keyboard getOrderManagementKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Инфо о заказе"))
                .setColor(KeyboardButtonColor.POSITIVE));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Смена данных/размера"))
                .setColor(KeyboardButtonColor.PRIMARY));
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Нет, отправил больше 2-х месяцев"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Возврат"))
                .setColor(KeyboardButtonColor.NEGATIVE));
        
        List<KeyboardButton> row4 = new ArrayList<>();
        row4.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Вернуться в меню"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        allButtons.add(row4);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру для ввода данных заказа.
     */
    public static Keyboard getOrderKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Отмена"))
                .setColor(KeyboardButtonColor.NEGATIVE));
        
        allButtons.add(row1);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру для диалога с администратором.
     */
    public static Keyboard getAdminKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Выйти из чата с админом"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        allButtons.add(row1);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру для работы с криптовалютами.
     */
    public static Keyboard getCryptoKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(createCryptoButton("BTC"));
        row1.add(createCryptoButton("ETH"));
        row1.add(createCryptoButton("SOL"));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(createCryptoButton("BNB"));
        row2.add(createCryptoButton("XRP"));
        row2.add(createCryptoButton("ADA"));
        
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(createCryptoButton("DOGE"));
        row3.add(createCryptoButton("MATIC"));
        row3.add(createCryptoButton("DOT"));
        
        List<KeyboardButton> row4 = new ArrayList<>();
        row4.add(createCryptoButton("TON"));
        row4.add(createCryptoButton("SHIB"));
        row4.add(createCryptoButton("INJ"));
        
        List<KeyboardButton> row5 = new ArrayList<>();
        row5.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Вернуться в меню"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        allButtons.add(row4);
        allButtons.add(row5);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру с трендовыми криптовалютами.
     */
    public static Keyboard getTrendingCryptoKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(createCryptoButton("BTC"));
        row1.add(createCryptoButton("ETH"));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Все криптовалюты"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Вернуться в меню"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Вспомогательный метод для создания кнопки криптовалюты.
     */
    private static KeyboardButton createCryptoButton(String token) {
        return new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("$" + token))
                .setColor(KeyboardButtonColor.DEFAULT);
    }
    
    /**
     * Создает клавиатуру для работы с платежами
     */
    public static Keyboard getPaymentKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Оплатить заказ"))
                .setColor(KeyboardButtonColor.POSITIVE));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Отмена оплаты"))
                .setColor(KeyboardButtonColor.NEGATIVE));
        
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Вернуться в меню"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру для подтверждения платежей администратором
     */
    public static Keyboard getAdminPaymentKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Подтвердить оплату"))
                .setColor(KeyboardButtonColor.POSITIVE));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Отклонить оплату"))
                .setColor(KeyboardButtonColor.NEGATIVE));
        
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Возврат в меню админа"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру с каталогом товаров для выбора
     */
    public static Keyboard getProductCatalogKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Цифровые товары"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Физические товары"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Услуги"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        List<KeyboardButton> row4 = new ArrayList<>();
        row4.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Отмена"))
                .setColor(KeyboardButtonColor.NEGATIVE));
        
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        allButtons.add(row4);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру для выбора цифровых товаров
     */
    public static Keyboard getDigitalProductsKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Аккаунты VPN"))
                .setColor(KeyboardButtonColor.DEFAULT));
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Подписки"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Ключи Steam"))
                .setColor(KeyboardButtonColor.DEFAULT));
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Программы"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Назад"))
                .setColor(KeyboardButtonColor.PRIMARY));
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Отмена"))
                .setColor(KeyboardButtonColor.NEGATIVE));
        
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру для выбора физических товаров
     */
    public static Keyboard getPhysicalProductsKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Одежда"))
                .setColor(KeyboardButtonColor.DEFAULT));
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Обувь"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Аксессуары"))
                .setColor(KeyboardButtonColor.DEFAULT));
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Техника"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Назад"))
                .setColor(KeyboardButtonColor.PRIMARY));
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Отмена"))
                .setColor(KeyboardButtonColor.NEGATIVE));
        
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру для выбора услуг
     */
    public static Keyboard getServicesKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Консультации"))
                .setColor(KeyboardButtonColor.DEFAULT));
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Дизайн"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Программирование"))
                .setColor(KeyboardButtonColor.DEFAULT));
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Перевод текста"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Назад"))
                .setColor(KeyboardButtonColor.PRIMARY));
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Отмена"))
                .setColor(KeyboardButtonColor.NEGATIVE));
        
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает клавиатуру для профиля пользователя
     */
    public static Keyboard getProfileKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("✏️ Изменить данные"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("🔔 Управление уведомлениями"))
                .setColor(KeyboardButtonColor.DEFAULT));
        
        List<KeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("💵 Пополнить баланс"))
                .setColor(KeyboardButtonColor.POSITIVE));
        
        List<KeyboardButton> row4 = new ArrayList<>();
        row4.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Вернуться в меню"))
                .setColor(KeyboardButtonColor.PRIMARY));
                
        allButtons.add(row1);
        allButtons.add(row2);
        allButtons.add(row3);
        allButtons.add(row4);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
    
    /**
     * Создает универсальную клавиатуру с кнопкой возврата в главное меню
     */
    public static Keyboard getBackToMenuKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Вернуться в меню"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        allButtons.add(row1);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }
} 