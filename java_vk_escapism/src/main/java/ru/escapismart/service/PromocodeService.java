package ru.escapismart.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.escapismart.dao.PromocodeDao;
import ru.escapismart.dao.UserDao;
import ru.escapismart.model.Promocode;
import ru.escapismart.model.User;

/**
 * Сервис для работы с промокодами
 */
public class PromocodeService {
    private static final Logger logger = LoggerFactory.getLogger(PromocodeService.class);
    private static PromocodeService instance;
    
    private final PromocodeDao promocodeDao;
    private final UserDao userDao;
    
    /**
     * Приватный конструктор (Singleton)
     */
    private PromocodeService() {
        promocodeDao = new PromocodeDao();
        userDao = new UserDao();
        logger.info("PromocodeService инициализирован");
    }
    
    /**
     * Получить экземпляр сервиса
     */
    public static synchronized PromocodeService getInstance() {
        if (instance == null) {
            instance = new PromocodeService();
        }
        return instance;
    }
    
    /**
     * Проверить существование и валидность промокода
     * @param code код промокода
     * @return промокод или null, если не найден или не действителен
     */
    public Promocode validatePromocode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        
        Promocode promocode = promocodeDao.findByCode(code.toUpperCase());
        
        if (promocode == null) {
            logger.info("Промокод не найден: {}", code);
            return null;
        }
        
        if (!promocode.isValid()) {
            logger.info("Промокод недействителен: {}", code);
            return null;
        }
        
        return promocode;
    }
    
    /**
     * Применить промокод к заказу пользователя
     * @param userId ID пользователя
     * @param code код промокода
     * @param orderAmount сумма заказа
     * @return сумма скидки или 0, если промокод не применен
     */
    public double applyPromocode(Long userId, String code, double orderAmount) {
        User user = userDao.getUser(userId);
        if (user == null) {
            logger.error("Пользователь не найден: {}", userId);
            return 0;
        }
        
        // Проверяем, использовал ли пользователь этот промокод ранее
        if (user.hasUsedPromocode(code.toUpperCase())) {
            logger.info("Пользователь {} уже использовал промокод {}", userId, code);
            return 0;
        }
        
        Promocode promocode = validatePromocode(code);
        if (promocode == null) {
            return 0;
        }
        
        // Рассчитываем скидку
        double discount = promocode.calculateDiscount(orderAmount);
        
        if (discount > 0) {
            // Отмечаем использование промокода
            user.addUsedPromocode(code.toUpperCase());
            userDao.save(user);
            
            // Увеличиваем счетчик использований промокода
            promocodeDao.incrementUses(code);
            
            logger.info("Промокод {} применен для пользователя {}, скидка: {}", code, userId, discount);
        }
        
        return discount;
    }
    
    /**
     * Создать новый промокод с процентной скидкой
     * @param code код промокода
     * @param discountPercent процент скидки
     * @return созданный промокод или null в случае ошибки
     */
    public Promocode createPercentPromocode(String code, int discountPercent) {
        try {
            // Проверяем, нет ли уже такого промокода
            Promocode existing = promocodeDao.findByCode(code.toUpperCase());
            if (existing != null) {
                logger.warn("Промокод с кодом {} уже существует", code);
                return null;
            }
            
            // Создаем новый промокод
            Promocode promocode = new Promocode(code, discountPercent);
            
            if (promocodeDao.save(promocode)) {
                logger.info("Создан новый процентный промокод: {}, скидка: {}%", code, discountPercent);
                return promocode;
            } else {
                logger.error("Не удалось сохранить промокод: {}", code);
                return null;
            }
        } catch (Exception e) {
            logger.error("Ошибка при создании промокода: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Создать новый промокод с фиксированной скидкой
     * @param code код промокода
     * @param discountAmount сумма скидки
     * @return созданный промокод или null в случае ошибки
     */
    public Promocode createFixedPromocode(String code, double discountAmount) {
        try {
            // Проверяем, нет ли уже такого промокода
            Promocode existing = promocodeDao.findByCode(code.toUpperCase());
            if (existing != null) {
                logger.warn("Промокод с кодом {} уже существует", code);
                return null;
            }
            
            // Создаем новый промокод
            Promocode promocode = new Promocode(code, discountAmount);
            
            if (promocodeDao.save(promocode)) {
                logger.info("Создан новый фиксированный промокод: {}, скидка: {} руб.", code, discountAmount);
                return promocode;
            } else {
                logger.error("Не удалось сохранить промокод: {}", code);
                return null;
            }
        } catch (Exception e) {
            logger.error("Ошибка при создании промокода: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Деактивировать промокод
     * @param code код промокода
     * @return true если операция успешна
     */
    public boolean deactivatePromocode(String code) {
        boolean result = promocodeDao.deactivate(code);
        if (result) {
            logger.info("Промокод {} деактивирован", code);
        } else {
            logger.warn("Не удалось деактивировать промокод: {}", code);
        }
        return result;
    }
} 