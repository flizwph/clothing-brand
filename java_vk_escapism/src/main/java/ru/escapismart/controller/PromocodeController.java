package ru.escapismart.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.escapismart.model.Order;
import ru.escapismart.model.Promocode;
import ru.escapismart.model.User;
import ru.escapismart.service.PromocodeService;
import ru.escapismart.dao.OrderDao;
import ru.escapismart.dao.UserDao;

/**
 * Контроллер для работы с промокодами
 */
public class PromocodeController {
    private static final Logger logger = LoggerFactory.getLogger(PromocodeController.class);
    private static PromocodeController instance;
    
    private final PromocodeService promocodeService;
    private final UserDao userDao;
    private final OrderDao orderDao;
    
    /**
     * Приватный конструктор для реализации Singleton
     */
    private PromocodeController() {
        this.promocodeService = PromocodeService.getInstance();
        this.userDao = new UserDao();
        this.orderDao = new OrderDao();
    }
    
    /**
     * Получить экземпляр контроллера
     */
    public static synchronized PromocodeController getInstance() {
        if (instance == null) {
            instance = new PromocodeController();
        }
        return instance;
    }
    
    /**
     * Создать новый процентный промокод
     * @param code код промокода
     * @param discountPercent процент скидки
     * @return сообщение о результате операции
     */
    public String createPercentPromocode(String code, int discountPercent) {
        if (code == null || code.isEmpty()) {
            return "Ошибка: Код промокода не может быть пустым";
        }
        
        if (discountPercent <= 0 || discountPercent > 100) {
            return "Ошибка: Процент скидки должен быть в пределах от 1 до 100";
        }
        
        Promocode promocode = promocodeService.createPercentPromocode(code, discountPercent);
        
        if (promocode != null) {
            return String.format("Процентный промокод %s успешно создан. Скидка: %d%%", code, discountPercent);
        } else {
            return String.format("Ошибка: Не удалось создать промокод %s", code);
        }
    }
    
    /**
     * Создать новый фиксированный промокод
     * @param code код промокода
     * @param discountAmount сумма скидки
     * @return сообщение о результате операции
     */
    public String createFixedPromocode(String code, double discountAmount) {
        if (code == null || code.isEmpty()) {
            return "Ошибка: Код промокода не может быть пустым";
        }
        
        if (discountAmount <= 0) {
            return "Ошибка: Сумма скидки должна быть положительной";
        }
        
        Promocode promocode = promocodeService.createFixedPromocode(code, discountAmount);
        
        if (promocode != null) {
            return String.format("Фиксированный промокод %s успешно создан. Скидка: %.2f руб.", code, discountAmount);
        } else {
            return String.format("Ошибка: Не удалось создать промокод %s", code);
        }
    }
    
    /**
     * Применить промокод к заказу
     * @param userId ID пользователя
     * @param orderId ID заказа
     * @param code код промокода
     * @return сообщение о результате операции
     */
    public String applyPromocodeToOrder(Long userId, Long orderId, String code) {
        if (code == null || code.isEmpty()) {
            return "Ошибка: Код промокода не может быть пустым";
        }
        
        User user = userDao.getUser(userId);
        if (user == null) {
            return "Ошибка: Пользователь не найден";
        }
        
        Order order = orderDao.getOrderById(orderId);
        if (order == null) {
            return "Ошибка: Заказ не найден";
        }
        
        if (order.getAppliedPromocode() != null && !order.getAppliedPromocode().isEmpty()) {
            return "Ошибка: К заказу уже применен промокод " + order.getAppliedPromocode();
        }
        
        if (user.hasUsedPromocode(code.toUpperCase())) {
            return "Ошибка: Вы уже использовали промокод " + code;
        }
        
        double orderAmount = order.getAmount() != null ? order.getAmount() : 0.0;
        double discount = promocodeService.applyPromocode(userId, code, orderAmount);
        
        if (discount > 0) {
            order.applyPromocode(code.toUpperCase(), discount);
            orderDao.updateOrder(order);
            
            return String.format("Промокод %s успешно применен. Скидка: %.2f руб.", code, discount);
        } else {
            return String.format("Ошибка: Не удалось применить промокод %s", code);
        }
    }
    
    /**
     * Деактивировать промокод
     * @param code код промокода
     * @return сообщение о результате операции
     */
    public String deactivatePromocode(String code) {
        if (code == null || code.isEmpty()) {
            return "Ошибка: Код промокода не может быть пустым";
        }
        
        boolean success = promocodeService.deactivatePromocode(code);
        
        if (success) {
            return String.format("Промокод %s успешно деактивирован", code);
        } else {
            return String.format("Ошибка: Не удалось деактивировать промокод %s", code);
        }
    }
    
    /**
     * Проверить валидность промокода
     * @param code код промокода
     * @return сообщение с информацией о промокоде
     */
    public String checkPromocode(String code) {
        if (code == null || code.isEmpty()) {
            return "Ошибка: Код промокода не может быть пустым";
        }
        
        Promocode promocode = promocodeService.validatePromocode(code);
        
        if (promocode == null) {
            return String.format("Промокод %s не найден или недействителен", code);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Промокод %s найден и действителен.%n", code));
        
        if (promocode.getDiscountPercent() != null && promocode.getDiscountPercent() > 0) {
            sb.append(String.format("Тип: Процентная скидка %d%%%n", promocode.getDiscountPercent()));
        } else if (promocode.getDiscountAmount() != null && promocode.getDiscountAmount() > 0) {
            sb.append(String.format("Тип: Фиксированная скидка %.2f руб.%n", promocode.getDiscountAmount()));
        }
        
        if (promocode.getMinOrderAmount() != null && promocode.getMinOrderAmount() > 0) {
            sb.append(String.format("Минимальная сумма заказа: %.2f руб.%n", promocode.getMinOrderAmount()));
        }
        
        if (promocode.getMaxUses() != null) {
            sb.append(String.format("Использований: %d из %d%n", promocode.getCurrentUses(), promocode.getMaxUses()));
        }
        
        if (promocode.getValidUntil() != null) {
            sb.append(String.format("Действителен до: %s%n", promocode.getValidUntil().toString()));
        }
        
        if (promocode.getDescription() != null && !promocode.getDescription().isEmpty()) {
            sb.append(String.format("Описание: %s%n", promocode.getDescription()));
        }
        
        return sb.toString();
    }
} 