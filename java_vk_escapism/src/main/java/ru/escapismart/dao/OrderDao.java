package ru.escapismart.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import ru.escapismart.model.Order;
import ru.escapismart.util.HibernateUtil;

public class OrderDao {
    private final SessionFactory sessionFactory;
    
    public OrderDao() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }
    
    public Long saveOrder(Long vkId, String orderData, String status) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            
            if (vkId == null) {
                vkId = 0L;
            }
            
            Order order = new Order(vkId, orderData);
            order.setStatus(status);
            Long orderId = (Long) session.save(order);
            
            transaction.commit();
            return orderId;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при сохранении заказа: " + e.getMessage(), e);
        }
    }
    
    public void updateOrderStatus(Long vkId, String newStatus) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            
            if (vkId == null) {
                vkId = 0L;
            }
            
            String hql = "FROM Order o WHERE o.userVkId = :vkId ORDER BY o.createdAt DESC";
            Query<Order> query = session.createQuery(hql, Order.class);
            query.setParameter("vkId", vkId);
            query.setMaxResults(1);
            
            Order latestOrder = query.uniqueResult();
            if (latestOrder != null) {
                latestOrder.setStatus(newStatus);
                session.update(latestOrder);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при обновлении статуса заказа: " + e.getMessage(), e);
        }
    }
    
    public Order getLatestOrder(Long vkId) {
        try (Session session = sessionFactory.openSession()) {
            if (vkId == null) {
                vkId = 0L;
            }
            
            String hql = "FROM Order o WHERE o.userVkId = :vkId ORDER BY o.createdAt DESC";
            Query<Order> query = session.createQuery(hql, Order.class);
            query.setParameter("vkId", vkId);
            query.setMaxResults(1);
            
            return query.uniqueResult();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении заказа: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получить заказ по ID
     * @param orderId ID заказа
     * @return заказ или null, если не найден
     */
    public Order getOrderById(Long orderId) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Order.class, orderId);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении заказа по ID: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обновить заказ
     * @param order заказ для обновления
     */
    public void updateOrder(Order order) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.update(order);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при обновлении заказа: " + e.getMessage(), e);
        }
    }
    
    /**
     * Безопасное сопоставление идентификатора пользователя
     * @param vkId идентификатор пользователя в VK (может быть null, String или Long)
     * @return Long идентификатор, безопасный для использования (0L если null)
     */
    public Long safeUserId(Object vkId) {
        if (vkId == null) {
            return 0L;
        }
        
        if (vkId instanceof String) {
            try {
                return Long.parseLong((String) vkId);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        
        if (vkId instanceof Integer) {
            return ((Integer) vkId).longValue();
        }
        
        if (vkId instanceof Long) {
            return (Long) vkId;
        }
        
        return 0L;
    }
} 