package com.brand.backend.infrastructure.integration.telegram.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO для статистики заказов
 */
@Data
@Builder
public class OrderStatisticsDto {
    // Общая статистика
    private int totalOrders;
    private BigDecimal totalRevenue;
    private int totalCompletedOrders;
    private int totalCancelledOrders;
    
    // Статистика по статусам
    private int newOrders;
    private int processingOrders;
    private int dispatchedOrders;
    private int completedOrders;
    private int cancelledOrders;
    
    // Статистика по периодам
    private int ordersToday;
    private int ordersThisWeek;
    private int ordersThisMonth;
    
    // Средние значения
    private BigDecimal averageOrderValue;
} 