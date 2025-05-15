package ru.escapismart.dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.escapismart.model.Promocode;
import ru.escapismart.util.HibernateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO для работы с промокодами
 */
public class PromocodeDao {
    private static final Logger logger = LoggerFactory.getLogger(PromocodeDao.class);
    
    /**
     * Сохранить или обновить промокод
     * @param promocode объект промокода
     * @return true если операция успешна
     */
    public boolean save(Promocode promocode) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(promocode);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("Ошибка при сохранении промокода: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Найти промокод по коду
     * @param code код промокода
     * @return объект промокода или null
     */
    public Promocode findByCode(String code) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Promocode> query = session.createQuery(
                "from Promocode where code = :code", Promocode.class);
            query.setParameter("code", code.toUpperCase());
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Ошибка при поиске промокода по коду: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Получить все активные промокоды
     * @return список активных промокодов
     */
    public List<Promocode> findAllActive() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Promocode> query = session.createQuery(
                "from Promocode where isActive = true", Promocode.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Ошибка при получении активных промокодов: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Получить промокод по ID
     * @param id идентификатор промокода
     * @return объект промокода или null
     */
    public Promocode findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Promocode.class, id);
        } catch (Exception e) {
            logger.error("Ошибка при получении промокода по ID: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Удалить промокод
     * @param promocode объект промокода
     * @return true если операция успешна
     */
    public boolean delete(Promocode promocode) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.delete(promocode);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("Ошибка при удалении промокода: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Деактивировать промокод
     * @param code код промокода
     * @return true если операция успешна
     */
    public boolean deactivate(String code) {
        Promocode promocode = findByCode(code);
        if (promocode == null) {
            return false;
        }
        
        promocode.setActive(false);
        return save(promocode);
    }
    
    /**
     * Увеличить счетчик использований промокода
     * @param code код промокода
     * @return true если операция успешна
     */
    public boolean incrementUses(String code) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Promocode promocode = findByCode(code);
            if (promocode == null) {
                return false;
            }
            
            transaction = session.beginTransaction();
            promocode.incrementUses();
            session.update(promocode);
            
            // Если достигнут лимит использований, деактивируем промокод
            if (promocode.getMaxUses() != null && promocode.getCurrentUses() >= promocode.getMaxUses()) {
                promocode.setActive(false);
            }
            
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("Ошибка при увеличении счетчика использований промокода: {}", e.getMessage(), e);
            return false;
        }
    }
} 