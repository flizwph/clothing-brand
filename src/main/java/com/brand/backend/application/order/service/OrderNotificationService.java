package com.brand.backend.application.order.service;

import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.order.model.DigitalOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderNotificationService {

    public void notifyNewOrder(Order order) {
        log.info("Уведомление о новом заказе: {}", order.getOrderNumber());
        // TODO: Implement notification logic
    }
    
    public void notifyNewDigitalOrder(DigitalOrder order) {
        log.info("Уведомление о новом цифровом заказе: {}", order.getOrderNumber());
        // TODO: Implement notification logic
    }
} 