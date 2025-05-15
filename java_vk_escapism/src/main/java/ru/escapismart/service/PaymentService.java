package ru.escapismart.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.escapismart.config.AppConfig;
import ru.escapismart.dao.PaymentDao;
import ru.escapismart.model.Payment;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Сервис для работы с платежами
 * Обеспечивает создание, поиск и управление платежами пользователей
 */
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    private static final String PAYMENT_PENDING = "PENDING";
    private static final String PAYMENT_COMPLETED = "COMPLETED";
    private static final String PAYMENT_CANCELLED = "CANCELLED";
    
    private final PaymentDao paymentDao;
    private final AppConfig appConfig;
    
    public PaymentService() {
        this.paymentDao = new PaymentDao();
        this.appConfig = AppConfig.getInstance();
    }
    
    /**
     * Создать новый платеж
     * @param userId ID пользователя
     * @param orderId ID заказа в виде строки (может быть числом или произвольной строкой)
     * @param amount Сумма платежа
     * @param comment Комментарий платежа
     * @return Созданный объект платежа
     */
    public Payment createPayment(Long userId, String orderId, BigDecimal amount, String comment) {
        try {
            // Генерация уникального идентификатора платежа
            String paymentComment = comment != null ? comment : generatePaymentComment();
            
            // Создание объекта платежа
            Payment payment = new Payment();
            payment.setUserId(userId);
            payment.setOrderId(orderId);
            payment.setAmount(amount);
            payment.setPaymentComment(paymentComment);
            payment.setPaymentAccount(appConfig.getPaymentAccount());
            payment.setStatus("PENDING");
            payment.setCreatedAt(new Date());
            payment.setUpdatedAt(new Date());
            
            // Сохранение платежа в базе данных с новой сессией
            Payment savedPayment = paymentDao.save(payment);
            logger.info("Создан новый платеж: {}", savedPayment.getId());
            
            return savedPayment;
        } catch (Exception e) {
            logger.error("Ошибка при создании платежа: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось создать платеж", e);
        }
    }
    
    /**
     * Получить платеж по ID
     * @param paymentId ID платежа
     * @return Платеж
     */
    public Payment getPaymentById(Long paymentId) {
        try {
            Payment payment = paymentDao.getById(paymentId);
            if (payment == null) {
                logger.warn("Платеж с ID {} не найден", paymentId);
                return null;
            }
            return payment;
        } catch (Exception e) {
            logger.error("Ошибка при получении платежа по ID: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось получить платеж", e);
        }
    }
    
    /**
     * Получить все платежи пользователя
     * @param userId ID пользователя
     * @return Список платежей пользователя
     */
    public List<Payment> getPaymentsByUserId(Long userId) {
        try {
            List<Payment> payments = paymentDao.getByUserId(userId);
            logger.info("Получено {} платежей для пользователя {}", payments.size(), userId);
            return payments;
        } catch (Exception e) {
            logger.error("Ошибка при получении платежей пользователя: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось получить платежи пользователя", e);
        }
    }
    
    /**
     * Получить платеж по комментарию
     * @param comment Комментарий платежа
     * @return Платеж
     */
    public Payment getPaymentByComment(String comment) {
        try {
            Payment payment = paymentDao.getByComment(comment);
            if (payment == null) {
                logger.warn("Платеж с комментарием {} не найден", comment);
                return null;
            }
            return payment;
        } catch (Exception e) {
            logger.error("Ошибка при получении платежа по комментарию: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось получить платеж", e);
        }
    }
    
    /**
     * Отметить платеж как выполненный
     * @param paymentId ID платежа
     * @return true, если статус успешно обновлен
     */
    public boolean completePayment(Long paymentId) {
        return paymentDao.updateStatus(paymentId, PAYMENT_COMPLETED);
    }
    
    /**
     * Отменить платеж
     * @param paymentId ID платежа
     * @return true, если статус успешно обновлен
     */
    public boolean cancelPayment(Long paymentId) {
        return paymentDao.updateStatus(paymentId, PAYMENT_CANCELLED);
    }
    
    /**
     * Получить все ожидающие платежи
     * @return Список платежей со статусом PENDING
     */
    public List<Payment> getPendingPayments() {
        return paymentDao.findByStatus(PAYMENT_PENDING);
    }
    
    /**
     * Обновление статуса платежа
     * @param paymentId ID платежа
     * @param status Новый статус платежа
     * @return Обновленный платеж
     */
    public Payment updatePaymentStatus(Long paymentId, String status) {
        try {
            Payment payment = paymentDao.getById(paymentId);
            if (payment == null) {
                logger.warn("Платеж с ID {} не найден при обновлении статуса", paymentId);
                return null;
            }
            
            payment.setStatus(status);
            payment.setUpdatedAt(new Date());
            paymentDao.save(payment);
            
            logger.info("Обновлен статус платежа {}: {}", paymentId, status);
            return payment;
        } catch (Exception e) {
            logger.error("Ошибка при обновлении статуса платежа: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обновить статус платежа", e);
        }
    }
    
    /**
     * Генерировать уникальный комментарий для платежа
     * Формат: VKE + 6 случайных цифр
     * @return Уникальный комментарий, который отсутствует в базе данных
     */
    private String generatePaymentComment() {
        Random random = new Random();
        // Префикс VKE + 6 цифр
        String comment = "VKE" + String.format("%06d", random.nextInt(1000000));
        
        // Проверка на уникальность
        Payment payment = paymentDao.findByComment(comment);
        while (payment != null && payment.getId() != null) {
            comment = "VKE" + String.format("%06d", random.nextInt(1000000));
            payment = paymentDao.findByComment(comment);
        }
        
        return comment;
    }
} 