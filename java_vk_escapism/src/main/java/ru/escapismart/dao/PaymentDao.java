package ru.escapismart.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import ru.escapismart.model.Payment;
import ru.escapismart.util.HibernateUtil;

import java.util.List;
import java.util.Optional;

/**
 * DAO класс для работы с платежами в базе данных
 * Предоставляет методы для создания, чтения, обновления и удаления платежей
 * в системе VK Escapism Bot. Реализует паттерн DAO (Data Access Object) для
 * абстрагирования логики работы с базой данных от бизнес-логики приложения.
 */
public class PaymentDao {
    private final SessionFactory sessionFactory;

    /**
     * Конструктор, инициализирующий соединение с базой данных
     * через Hibernate SessionFactory
     */
    public PaymentDao() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    /**
     * Сохраняет новый платеж в базе данных
     * 
     * @param payment Объект платежа для сохранения
     * @return Сохраненный платеж с присвоенным ID
     * @throws RuntimeException при ошибке сохранения платежа
     */
    public Payment save(Payment payment) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.save(payment);
            transaction.commit();
            return payment;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при сохранении платежа: " + e.getMessage(), e);
        }
    }

    /**
     * Обновляет существующий платеж в базе данных
     * 
     * @param payment Объект платежа с обновленными данными
     * @throws RuntimeException при ошибке обновления платежа
     */
    public void update(Payment payment) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.update(payment);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при обновлении платежа: " + e.getMessage(), e);
        }
    }

    /**
     * Получает платеж по его ID
     * 
     * @param id ID платежа
     * @return Объект платежа или null, если платеж не найден
     */
    public Payment findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Payment.class, id);
        }
    }
    
    /**
     * Получает платеж по его ID (альтернативное название для совместимости)
     */
    public Payment getById(Long id) {
        return findById(id);
    }

    /**
     * Получает все платежи конкретного пользователя
     * 
     * @param userVkId ID пользователя VK
     * @return Список платежей пользователя
     */
    public List<Payment> findByUserVkId(Long userVkId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Payment WHERE userVkId = :userVkId", Payment.class)
                    .setParameter("userVkId", userVkId)
                    .list();
        }
    }
    
    /**
     * Получает все платежи пользователя (альтернативное название для совместимости)
     */
    public List<Payment> getByUserId(Long userVkId) {
        return findByUserVkId(userVkId);
    }

    /**
     * Получает платеж по уникальному комментарию
     * 
     * @param comment Уникальный комментарий платежа
     * @return Объект платежа или null, если платеж не найден
     */
    public Payment findByComment(String comment) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Payment WHERE paymentComment = :comment", Payment.class)
                    .setParameter("comment", comment)
                    .uniqueResult();
        }
    }
    
    /**
     * Получает платеж по комментарию (альтернативное название для совместимости)
     */
    public Payment getByComment(String comment) {
        return findByComment(comment);
    }

    /**
     * Получает все платежи в статусе ожидания
     * 
     * @return Список платежей в статусе PENDING
     */
    public List<Payment> findPendingPayments() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Payment WHERE status = 'PENDING'", Payment.class)
                    .list();
        }
    }

    /**
     * Удаляет платеж из базы данных
     * 
     * @param payment Объект платежа для удаления
     * @throws RuntimeException при ошибке удаления платежа
     */
    public void delete(Payment payment) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.delete(payment);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при удалении платежа: " + e.getMessage(), e);
        }
    }

    /**
     * Получает все платежи с определенным статусом
     * 
     * @param status Статус платежа для поиска (PENDING, COMPLETED, CANCELLED и т.д.)
     * @return Список платежей с указанным статусом
     */
    public List<Payment> findByStatus(String status) {
        try (Session session = sessionFactory.openSession()) {
            Query<Payment> query = session.createQuery(
                "FROM Payment WHERE status = :status", 
                Payment.class
            );
            query.setParameter("status", status);
            return query.list();
        }
    }

    /**
     * Обновляет статус существующего платежа
     * 
     * @param paymentId ID платежа для обновления
     * @param newStatus Новый статус платежа
     * @return true если платеж обновлен, false если платеж не найден
     * @throws RuntimeException при ошибке обновления статуса
     */
    public boolean updateStatus(Long paymentId, String newStatus) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Query<?> query = session.createQuery(
                "UPDATE Payment SET status = :status, updatedAt = CURRENT_TIMESTAMP " +
                "WHERE id = :id"
            );
            query.setParameter("status", newStatus);
            query.setParameter("id", paymentId);
            
            int result = query.executeUpdate();
            transaction.commit();
            return result > 0;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при обновлении статуса платежа: " + e.getMessage(), e);
        }
    }
} 