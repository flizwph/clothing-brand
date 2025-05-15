package ru.escapismart.dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.escapismart.model.CryptoAlert;
import ru.escapismart.util.HibernateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO для работы с подписками на уведомления изменения курсов криптовалют
 */
public class CryptoAlertDao {
    private static final Logger logger = LoggerFactory.getLogger(CryptoAlertDao.class);
    
    /**
     * Сохранить или обновить подписку
     * @param alert Объект подписки
     * @return true если операция успешна
     */
    public boolean save(CryptoAlert alert) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(alert);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("Ошибка при сохранении подписки: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Получить подписку по ID
     * @param id Идентификатор подписки
     * @return Объект подписки или null
     */
    public CryptoAlert getById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(CryptoAlert.class, id);
        } catch (Exception e) {
            logger.error("Ошибка при получении подписки по ID: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Получить подписку для пользователя и токена
     * @param userId ID пользователя
     * @param tokenSymbol Символ токена
     * @return Объект подписки или null
     */
    public CryptoAlert getByUserAndToken(Long userId, String tokenSymbol) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<CryptoAlert> query = session.createQuery(
                "from CryptoAlert where userId = :userId and tokenSymbol = :tokenSymbol", 
                CryptoAlert.class);
            query.setParameter("userId", userId);
            query.setParameter("tokenSymbol", tokenSymbol.toUpperCase());
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Ошибка при получении подписки по пользователю и токену: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Получить все активные подписки для пользователя
     * @param userId ID пользователя
     * @return Список подписок
     */
    public List<CryptoAlert> getActiveByUser(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<CryptoAlert> query = session.createQuery(
                "from CryptoAlert where userId = :userId and active = true", 
                CryptoAlert.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (Exception e) {
            logger.error("Ошибка при получении подписок пользователя: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Получить все активные подписки для токена
     * @param tokenSymbol Символ токена
     * @return Список подписок
     */
    public List<CryptoAlert> getActiveByToken(String tokenSymbol) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<CryptoAlert> query = session.createQuery(
                "from CryptoAlert where tokenSymbol = :tokenSymbol and active = true", 
                CryptoAlert.class);
            query.setParameter("tokenSymbol", tokenSymbol.toUpperCase());
            return query.list();
        } catch (Exception e) {
            logger.error("Ошибка при получении подписок для токена: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Получить все активные подписки
     * @return Список всех активных подписок
     */
    public List<CryptoAlert> getAllActive() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<CryptoAlert> query = session.createQuery(
                "from CryptoAlert where active = true", 
                CryptoAlert.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Ошибка при получении всех активных подписок: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Удалить подписку
     * @param alert Объект подписки
     * @return true если операция успешна
     */
    public boolean delete(CryptoAlert alert) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.delete(alert);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("Ошибка при удалении подписки: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Деактивировать подписку
     * @param userId ID пользователя
     * @param tokenSymbol Символ токена
     * @return true если операция успешна
     */
    public boolean deactivate(Long userId, String tokenSymbol) {
        CryptoAlert alert = getByUserAndToken(userId, tokenSymbol);
        if (alert == null) {
            return false;
        }
        
        alert.setActive(false);
        return save(alert);
    }
    
    /**
     * Активировать подписку
     * @param userId ID пользователя
     * @param tokenSymbol Символ токена
     * @param threshold Порог изменения цены
     * @return true если операция успешна
     */
    public boolean activateOrCreate(Long userId, String tokenSymbol, double threshold) {
        CryptoAlert alert = getByUserAndToken(userId, tokenSymbol);
        if (alert == null) {
            alert = new CryptoAlert(userId, tokenSymbol, threshold);
        } else {
            alert.setActive(true);
            alert.setThreshold(threshold);
        }
        
        return save(alert);
    }
} 