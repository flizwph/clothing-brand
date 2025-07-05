package com.brand.backend.domain.order.repository;

import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.order.model.OrderStatus;
import com.brand.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user); // ✅ Получение заказов текущего пользователя
    
    // Методы для статистики пользователя
    Long countByUser(User user); // Общее количество заказов пользователя
    Long countByUserAndStatusIn(User user, List<OrderStatus> statuses); // Количество заказов по статусам
    
    // Методы для статистики админ панели
    Long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    Long countByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);
    
    // Дополнительные методы для совместимости
    List<Order> findByUserId(Long userId);
}
