package ru.escapismart.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import ru.escapismart.model.User;
import ru.escapismart.util.HibernateUtil;

public class UserDao {
    private final SessionFactory sessionFactory;
    
    public UserDao() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }
    
    public User getUser(Long vkId) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(User.class, vkId);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении пользователя: " + e.getMessage(), e);
        }
    }
    
    public void save(User user) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            
            if (user.getId() == null) {
                session.save(user);
            } else {
                session.update(user);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при сохранении пользователя: " + e.getMessage(), e);
        }
    }
    
    public void updateUserState(Long vkId, String state) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            
            User user = session.get(User.class, vkId);
            if (user == null) {
                user = new User(vkId, state);
                session.save(user);
            } else {
                user.setLastState(state);
                session.update(user);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при обновлении пользователя: " + e.getMessage(), e);
        }
    }
    
    public String getUserState(Long vkId) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, vkId);
            return user != null ? user.getLastState() : null;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении состояния пользователя: " + e.getMessage(), e);
        }
    }
    
    public void updateUserBalance(Long vkId, Double amount) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            
            User user = session.get(User.class, vkId);
            if (user == null) {
                user = new User(vkId, "INITIAL");
                user.setBalance(amount);
                session.save(user);
            } else {
                user.addToBalance(amount);
                session.update(user);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при обновлении баланса пользователя: " + e.getMessage(), e);
        }
    }
    
    public boolean deductUserBalance(Long vkId, Double amount) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, vkId);
            if (user == null || user.getBalance() == null || user.getBalance() < amount) {
                return false;
            }
            
            transaction = session.beginTransaction();
            boolean success = user.deductFromBalance(amount);
            
            if (success) {
                session.update(user);
                transaction.commit();
                return true;
            } else {
                transaction.rollback();
                return false;
            }
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при списании с баланса пользователя: " + e.getMessage(), e);
        }
    }
} 