package com.brand.backend.domain.order.repository;

import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user); // ✅ Получение заказов текущего пользователя
}
