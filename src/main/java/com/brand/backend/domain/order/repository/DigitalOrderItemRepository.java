package com.brand.backend.domain.order.repository;

import com.brand.backend.domain.order.model.DigitalOrderItem;
import com.brand.backend.domain.product.model.DigitalProduct;
import com.brand.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с элементами цифровых заказов
 */
@Repository
public interface DigitalOrderItemRepository extends JpaRepository<DigitalOrderItem, Long> {
    
    /**
     * Найти элементы цифровых заказов по пользователю
     * 
     * @param user пользователь
     * @return список элементов цифровых заказов
     */
    List<DigitalOrderItem> findByUser(User user);
    
    /**
     * Найти элементы цифровых заказов по пользователю и продукту
     * 
     * @param user пользователь
     * @param productId ID цифрового продукта
     * @return список элементов цифровых заказов
     */
    @Query("SELECT di FROM DigitalOrderItem di WHERE di.user = :user AND di.digitalProduct.id = :productId")
    List<DigitalOrderItem> findByUserAndProductId(@Param("user") User user, @Param("productId") Long productId);
    
    /**
     * Найти элемент цифрового заказа по коду активации
     * 
     * @param activationCode код активации
     * @return опциональный элемент цифрового заказа
     */
    Optional<DigitalOrderItem> findByActivationCode(String activationCode);
    
    /**
     * Найти активные элементы цифровых заказов пользователя
     * 
     * @param user пользователь
     * @param now текущее время
     * @return список активных элементов цифровых заказов
     */
    @Query("SELECT di FROM DigitalOrderItem di WHERE di.user = :user AND di.activationDate IS NOT NULL AND (di.expirationDate IS NULL OR di.expirationDate > :now)")
    List<DigitalOrderItem> findActiveItemsByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    /**
     * Найти истекшие элементы цифровых заказов
     * 
     * @param now текущее время
     * @return список истекших элементов цифровых заказов
     */
    @Query("SELECT di FROM DigitalOrderItem di WHERE di.activationDate IS NOT NULL AND di.expirationDate IS NOT NULL AND di.expirationDate < :now")
    List<DigitalOrderItem> findExpiredItems(@Param("now") LocalDateTime now);
    
    /**
     * Найти элементы цифровых заказов по продукту
     * 
     * @param product цифровой продукт
     * @return список элементов цифровых заказов
     */
    List<DigitalOrderItem> findByDigitalProduct(DigitalProduct product);
} 