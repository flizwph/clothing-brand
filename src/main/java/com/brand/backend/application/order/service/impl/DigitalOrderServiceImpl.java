package com.brand.backend.application.order.service.impl;

import com.brand.backend.application.order.service.DigitalOrderService;
import com.brand.backend.application.order.service.OrderNotificationService;
import com.brand.backend.common.exception.ResourceNotFoundException;
import com.brand.backend.domain.order.model.DigitalOrder;
import com.brand.backend.domain.order.model.DigitalOrderItem;
import com.brand.backend.domain.order.repository.DigitalOrderItemRepository;
import com.brand.backend.domain.order.repository.DigitalOrderRepository;
import com.brand.backend.domain.product.model.DigitalProduct;
import com.brand.backend.domain.product.repository.DigitalProductRepository;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.presentation.dto.request.DigitalOrderDto;
import com.brand.backend.presentation.dto.request.DigitalOrderItemDto;
import com.brand.backend.presentation.dto.response.DigitalOrderItemResponseDto;
import com.brand.backend.presentation.dto.response.DigitalOrderResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для работы с цифровыми заказами
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalOrderServiceImpl implements DigitalOrderService {

    private final DigitalOrderRepository digitalOrderRepository;
    private final DigitalOrderItemRepository digitalOrderItemRepository;
    private final DigitalProductRepository digitalProductRepository;
    private final UserRepository userRepository;
    private final OrderNotificationService orderNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public DigitalOrderResponseDto createOrder(String username, DigitalOrderDto orderDto) {
        log.info("Создание цифрового заказа для пользователя: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
        
        DigitalOrder order = DigitalOrder.builder()
                .user(user)
                .paymentMethod(orderDto.getPaymentMethod())
                .paid(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        List<DigitalOrderItem> orderItems = new ArrayList<>();
        
        for (DigitalOrderItemDto itemDto : orderDto.getItems()) {
            DigitalProduct product = digitalProductRepository.findById(itemDto.getDigitalProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Цифровой продукт", "id", itemDto.getDigitalProductId()));
            
            DigitalOrderItem item = DigitalOrderItem.builder()
                    .digitalProduct(product)
                    .user(user)
                    .quantity(itemDto.getQuantity())
                    .price(product.getPrice())
                    .activationCode(generateActivationCode())
                    .build();
            
            orderItems.add(item);
            order.addItem(item);
        }
        
        order.calculateTotalPrice();
        DigitalOrder savedOrder = digitalOrderRepository.save(order);
        
        // Отправляем уведомление администраторам о новом цифровом заказе
        try {
            orderNotificationService.notifyNewDigitalOrder(savedOrder);
        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления о новом цифровом заказе: {}", e.getMessage(), e);
            // Не прерываем выполнение метода из-за ошибки отправки уведомления
        }
        
        return mapToResponseDto(savedOrder);
    }

    @Override
    public Optional<DigitalOrderResponseDto> getOrderById(Long id) {
        log.info("Получение цифрового заказа по ID: {}", id);
        
        return digitalOrderRepository.findById(id)
                .map(this::mapToResponseDto);
    }

    @Override
    public List<DigitalOrderResponseDto> getUserOrders(String username) {
        log.info("Получение всех цифровых заказов пользователя: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
        
        List<DigitalOrder> orders = digitalOrderRepository.findAllByUserOrderByCreatedAtDesc(user);
        
        return orders.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DigitalOrderItemResponseDto activateOrderItem(String username, Long itemId) {
        log.info("Активация позиции цифрового заказа: {} для пользователя: {}", itemId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
        
        DigitalOrderItem item = digitalOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Позиция цифрового заказа", "id", itemId));
        
        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Пользователь не имеет прав на активацию данного продукта");
        }
        
        if (!item.getOrder().isPaid()) {
            throw new IllegalStateException("Невозможно активировать продукт в неоплаченном заказе");
        }
        
        item.activate();
        DigitalOrderItem savedItem = digitalOrderItemRepository.save(item);
        
        return mapToItemResponseDto(savedItem);
    }
    
    /**
     * Преобразует модель DigitalOrder в DTO ответа
     */
    private DigitalOrderResponseDto mapToResponseDto(DigitalOrder order) {
        List<DigitalOrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(this::mapToItemResponseDto)
                .collect(Collectors.toList());
        
        return DigitalOrderResponseDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .totalPrice(order.getTotalPrice())
                .paid(order.isPaid())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .paymentDate(order.getPaymentDate())
                .items(itemDtos)
                .build();
    }
    
    /**
     * Преобразует модель DigitalOrderItem в DTO ответа
     */
    private DigitalOrderItemResponseDto mapToItemResponseDto(DigitalOrderItem item) {
        return DigitalOrderItemResponseDto.builder()
                .id(item.getId())
                .digitalProductId(item.getDigitalProduct().getId())
                .productName(item.getDigitalProduct().getName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .activationCode(item.getActivationCode())
                .activationDate(item.getActivationDate())
                .expirationDate(item.getExpirationDate())
                .active(item.isActive())
                .build();
    }
    
    /**
     * Генерирует код активации для цифрового продукта
     */
    private String generateActivationCode() {
        byte[] randomBytes = new byte[18];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}