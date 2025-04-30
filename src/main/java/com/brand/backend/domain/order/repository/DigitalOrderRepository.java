package com.brand.backend.domain.order.repository;

import com.brand.backend.domain.order.model.DigitalOrder;
import com.brand.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с цифровыми заказами
 */
@Repository
public interface DigitalOrderRepository extends JpaRepository<DigitalOrder, Long> {
    
    /**
     * Найти все заказы пользователя
     * 
     * @param user пользователь
     * @return список заказов
     */
    List<DigitalOrder> findAllByUser(User user);
    
    /**
     * Найти все заказы пользователя, отсортированные по дате создания (по убыванию)
     * 
     * @param user пользователь
     * @return список заказов
     */
    List<DigitalOrder> findAllByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Найти заказ по номеру
     * 
     * @param orderNumber номер заказа
     * @return заказ
     */
    Optional<DigitalOrder> findByOrderNumber(String orderNumber);
    
    /**
     * Найти оплаченные заказы пользователя
     * 
     * @param user пользователь
     * @param paid статус оплаты
     * @return список заказов
     */
    List<DigitalOrder> findAllByUserAndPaid(User user, boolean paid);
    
    /**
     * Найти заказ по идентификатору платежа
     * 
     * @param paymentId идентификатор платежа
     * @return заказ
     */
    Optional<DigitalOrder> findByPaymentId(String paymentId);
    
    /**
     * Найти все заказы пользователя, сделанные после указанной даты
     * 
     * @param user пользователь
     * @param date дата, после которой сделаны заказы
     * @return список заказов
     */
    List<DigitalOrder> findByUserAndCreatedAtAfter(User user, LocalDateTime date);
    
    /**
     * Найти все заказы, созданные в указанный период
     * 
     * @param startDate начало периода
     * @param endDate конец периода
     * @return список заказов
     */
    @Query("SELECT o FROM DigitalOrder o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<DigitalOrder> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Найти заказы по статусу оплаты
     * 
     * @param isPaid статус оплаты
     * @return список заказов
     */
    List<DigitalOrder> findByPaid(boolean isPaid);
    
    /**
     * Найти количество заказов для каждого пользователя
     * 
     * @return список пар (пользователь, количество заказов)
     */
    @Query("SELECT o.user, COUNT(o) FROM DigitalOrder o GROUP BY o.user")
    List<Object[]> countOrdersByUser();
    
    /**
     * Найти сумму всех заказов пользователя
     * 
     * @param user пользователь
     * @return общая сумма заказов
     */
    @Query("SELECT SUM(o.totalPrice) FROM DigitalOrder o WHERE o.user = :user")
    Double sumTotalPriceByUser(@Param("user") User user);
} 