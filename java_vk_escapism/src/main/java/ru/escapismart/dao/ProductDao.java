package ru.escapismart.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import ru.escapismart.model.Product;
import ru.escapismart.util.HibernateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с товарами
 */
public class ProductDao {
    private final SessionFactory sessionFactory;
    
    public ProductDao() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }
    
    /**
     * Найти товары по категории
     * @param category категория товара
     * @return список товаров этой категории
     */
    public List<Product> findByCategory(String category) {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> query = session.createQuery("FROM Product WHERE category = :category", Product.class);
            query.setParameter("category", category);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске товаров по категории: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти товары по наличию на складе
     * @param inStock статус наличия
     * @return список товаров с указанным статусом
     */
    public List<Product> findByInStock(boolean inStock) {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> query = session.createQuery("FROM Product WHERE inStock = :inStock", Product.class);
            query.setParameter("inStock", inStock);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске товаров по наличию: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти товары по категории с статусом наличия
     * @param category категория товара
     * @param inStock статус наличия
     * @return список товаров категории с указанным статусом
     */
    public List<Product> findByCategoryAndInStock(String category, boolean inStock) {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> query = session.createQuery(
                "FROM Product WHERE category = :category AND inStock = :inStock", Product.class);
            query.setParameter("category", category);
            query.setParameter("inStock", inStock);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске товаров по категории и наличию: " + e.getMessage(), e);
        }
    }
    
    /**
     * Поиск товаров по имени (содержит подстроку)
     * @param namePart часть названия товара
     * @return список товаров, содержащих подстроку в названии
     */
    public List<Product> findByNameContainingIgnoreCase(String namePart) {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> query = session.createQuery(
                "FROM Product WHERE LOWER(name) LIKE LOWER(:namePart)", Product.class);
            query.setParameter("namePart", "%" + namePart + "%");
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске товаров по имени: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти товары со скидкой
     * @return список товаров со скидкой
     */
    public List<Product> findWithDiscount() {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> query = session.createQuery(
                "FROM Product WHERE discountPrice IS NOT NULL AND discountPrice > 0", Product.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске товаров со скидкой: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получить товар по ID
     * @param id ID товара
     * @return Optional с товаром или пустой, если не найден
     */
    public Optional<Product> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            Product product = session.get(Product.class, id);
            return Optional.ofNullable(product);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении товара по ID: " + e.getMessage(), e);
        }
    }
    
    /**
     * Сохранить или обновить товар
     * @param product товар для сохранения
     * @return сохраненный товар
     */
    public Product save(Product product) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            
            if (product.getId() == null) {
                session.save(product);
            } else {
                session.update(product);
            }
            
            transaction.commit();
            return product;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при сохранении товара: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удалить товар по ID
     * @param id ID товара
     */
    public void deleteById(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            
            Product product = session.get(Product.class, id);
            if (product != null) {
                session.delete(product);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при удалении товара: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получить все товары
     * @return список всех товаров
     */
    public List<Product> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> query = session.createQuery("FROM Product", Product.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении всех товаров: " + e.getMessage(), e);
        }
    }
} 