package com.brand.backend.application.order.service;

import com.brand.backend.presentation.dto.request.DigitalOrderDto;
import com.brand.backend.presentation.dto.response.DigitalOrderItemResponseDto;
import com.brand.backend.presentation.dto.response.DigitalOrderResponseDto;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с цифровыми заказами
 */
public interface DigitalOrderService {
    
    /**
     * Создание нового цифрового заказа
     * 
     * @param username имя пользователя
     * @param orderDto данные заказа
     * @return созданный заказ
     */
    DigitalOrderResponseDto createOrder(String username, DigitalOrderDto orderDto);
    
    /**
     * Получение цифрового заказа по ID
     * 
     * @param id ID заказа
     * @return заказ или пустой Optional, если заказ не найден
     */
    Optional<DigitalOrderResponseDto> getOrderById(Long id);
    
    /**
     * Получение всех цифровых заказов пользователя
     * 
     * @param username имя пользователя
     * @return список заказов
     */
    List<DigitalOrderResponseDto> getUserOrders(String username);
    
    /**
     * Активация позиции цифрового заказа
     * 
     * @param username имя пользователя
     * @param itemId ID позиции заказа
     * @return активированная позиция заказа
     */
    DigitalOrderItemResponseDto activateOrderItem(String username, Long itemId);
} 