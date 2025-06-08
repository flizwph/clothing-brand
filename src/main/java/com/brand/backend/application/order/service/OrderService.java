package com.brand.backend.application.order.service;

import com.brand.backend.application.order.util.OrderStatusUtil;
import com.brand.backend.application.promotion.service.PromoCodeService;
import com.brand.backend.presentation.dto.request.OrderDto;
import com.brand.backend.presentation.dto.response.DetailedOrderDTO;
import com.brand.backend.presentation.dto.response.OrderItemDTO;
import com.brand.backend.presentation.dto.response.OrderResponseDto;
import com.brand.backend.domain.order.event.OrderEvent;
import com.brand.backend.domain.order.model.DigitalOrder;
import com.brand.backend.domain.order.model.DigitalOrderItem;
import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.order.model.OrderStatus;
import com.brand.backend.domain.order.repository.DigitalOrderRepository;
import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.order.repository.OrderRepository;
import com.brand.backend.domain.product.repository.ProductRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DigitalOrderRepository digitalOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PromoCodeService promoCodeService;

    @Transactional
    public OrderResponseDto createOrder(String username, OrderDto orderDto) {
        log.info("Создание заказа для пользователя: {}", username);
        
        User user;
        try {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
            log.info("Пользователь найден в базе: {}, id: {}", username, user.getId());
        } catch (Exception e) {
            log.error("Ошибка при поиске пользователя {}: {}", username, e.getMessage(), e);
            throw e;
        }

        Product product = productRepository.findById(orderDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if(!isProductSizeAvailable(product, orderDto.getSize())) {
            throw new RuntimeException("Товар данного размера не доступен");
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8);

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setProduct(product);
        order.setQuantity(orderDto.getQuantity());
        order.setSize(orderDto.getSize());
        
        // Рассчитываем базовую цену
        double price = product.getPrice() * orderDto.getQuantity();
        
        // Применяем промокод, если он есть
        String promoCode = orderDto.getPromoCode();
        if (promoCode != null && !promoCode.isEmpty()) {
            try {
                if (promoCodeService.isPromoCodeValid(promoCode)) {
                    price = promoCodeService.applyPromoCode(promoCode, price);
                    log.info("Промокод {} применен к заказу {}. Новая цена: {}", 
                            promoCode, orderNumber, price);
                } else {
                    log.warn("Промокод {} недействителен для заказа {}", promoCode, orderNumber);
                }
            } catch (Exception e) {
                log.error("Ошибка при применении промокода {}: {}", promoCode, e.getMessage());
            }
        }
        
        order.setPrice(price);

        // Заполняем контактные данные заказа из OrderDto (которые уже могут совпадать с профилем)
        order.setEmail(orderDto.getEmail());
        order.setPhoneNumber(orderDto.getPhoneNumber());
        order.setFullName(orderDto.getFullName());
        order.setCountry(orderDto.getCountry());
        order.setAddress(orderDto.getAddress());
        order.setPostalCode(orderDto.getPostalCode());
        order.setTelegramUsername(orderDto.getTelegramUsername());
        order.setCryptoAddress(orderDto.getCryptoAddress());
        order.setOrderComment(orderDto.getOrderComment());
        order.setPromoCode(orderDto.getPromoCode());
        order.setPaymentMethod(orderDto.getPaymentMethod());
        order.setCreatedAt(LocalDateTime.now());
        order.setUser(user);

        order.setStatus(OrderStatus.NEW);

        Order savedOrder = orderRepository.save(order);

        reduceProductQuantity(product, orderDto.getSize());

        // Публикуем событие создания заказа
        eventPublisher.publishEvent(new OrderEvent(this, savedOrder, OrderEvent.OrderEventType.CREATED));

        log.info("✅ [ORDER CREATED] Заказ {} создан пользователем {}", orderNumber, username);
        return mapToDto(savedOrder);
    }

    public Optional<OrderResponseDto> getOrderById(Long id) {
        return orderRepository.findById(id).map(this::mapToDto);
    }

    public List<OrderResponseDto> getUserOrders(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return orderRepository.findByUser(user).stream().map(this::mapToDto).toList();
    }

    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Transactional
    public void cancelOrder(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (!order.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Вы не можете отменить этот заказ");
        }

        orderRepository.delete(order);
        
        // Публикуем событие отмены заказа
        eventPublisher.publishEvent(new OrderEvent(this, order, OrderEvent.OrderEventType.CANCELED));
        
        log.info("🗑 [ORDER CANCELED] Заказ {} отменен пользователем {}", order.getOrderNumber(), username);
    }
    
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        
        Order savedOrder = orderRepository.save(order);
        
        // Выбираем правильный тип события в зависимости от нового статуса
        OrderEvent.OrderEventType eventType;
        switch (newStatus) {
            case PROCESSING:
                eventType = OrderEvent.OrderEventType.PAID;
                break;
            case DISPATCHED:
                eventType = OrderEvent.OrderEventType.SHIPPED;
                break;
            case COMPLETED:
                eventType = OrderEvent.OrderEventType.DELIVERED;
                break;
            default:
                eventType = OrderEvent.OrderEventType.UPDATED;
                break;
        }
        
        // Публикуем событие обновления заказа
        eventPublisher.publishEvent(new OrderEvent(this, savedOrder, eventType));
        
        log.info("🔄 [ORDER STATUS UPDATED] Заказ {} обновлен с {} на {}", 
                order.getOrderNumber(), oldStatus, newStatus);
        
        return mapToDto(savedOrder);
    }

    private boolean isProductSizeAvailable(Product product, String size) {
        return switch (size.toLowerCase()) {
            case "m" -> product.getAvailableQuantityS() > 0;
            case "l" -> product.getAvailableQuantityM() > 0;
            case "xl" -> product.getAvailableQuantityL() > 0;
            default -> false;
        };
    }

    private void reduceProductQuantity(Product product, String size) {
        switch (size.toLowerCase()) {
            case "m" -> product.setAvailableQuantityS(product.getAvailableQuantityS() - 1);
            case "l" -> product.setAvailableQuantityM(product.getAvailableQuantityM() - 1);
            case "xl" -> product.setAvailableQuantityL(product.getAvailableQuantityL() - 1);
        }
        productRepository.save(product);
    }

    private OrderResponseDto mapToDto(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getProduct().getName(),
                order.getSize(),
                order.getQuantity(),
                order.getPrice(),
                order.getTelegramUsername(),
                order.getPaymentMethod(),
                order.getOrderComment(),
                order.getCreatedAt(),
                order.getStatus()
        );
    }

    /**
     * Получение активных заказов с детальной информацией
     */
    public List<DetailedOrderDTO> getActiveOrdersDetailed(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        List<DetailedOrderDTO> result = new ArrayList<>();
        
        // Получаем обычные заказы
        List<Order> orders = orderRepository.findByUser(user).stream()
                .filter(order -> OrderStatusUtil.isActiveOrder(order.getStatus()))
                .toList();
        
        for (Order order : orders) {
            result.add(mapToDetailedDTO(order));
        }
        
        // Получаем цифровые заказы
        List<DigitalOrder> digitalOrders = digitalOrderRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .filter(order -> !order.isPaid()) // Неоплаченные считаем активными
                .toList();
        
        for (DigitalOrder digitalOrder : digitalOrders) {
            result.add(mapToDetailedDTO(digitalOrder));
        }
        
        return result;
    }

    /**
     * Получение истории заказов с детальной информацией
     */
    public List<DetailedOrderDTO> getOrderHistoryDetailed(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        List<DetailedOrderDTO> result = new ArrayList<>();
        
        // Получаем обычные заказы
        List<Order> orders = orderRepository.findByUser(user).stream()
                .filter(order -> !OrderStatusUtil.isActiveOrder(order.getStatus()))
                .toList();
        
        for (Order order : orders) {
            result.add(mapToDetailedDTO(order));
        }
        
        // Получаем цифровые заказы
        List<DigitalOrder> digitalOrders = digitalOrderRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .filter(DigitalOrder::isPaid) // Оплаченные считаем историей
                .toList();
        
        for (DigitalOrder digitalOrder : digitalOrders) {
            result.add(mapToDetailedDTO(digitalOrder));
        }
        
        return result;
    }

    /**
     * Маппинг обычного заказа в DetailedOrderDTO
     */
    private DetailedOrderDTO mapToDetailedDTO(Order order) {
        List<OrderItemDTO> items = new ArrayList<>();
        
        // Для обычного заказа создаем один элемент
        OrderItemDTO item = new OrderItemDTO(
                order.getProduct().getId(),
                order.getProduct().getName(),
                "Одежда",
                order.getQuantity(),
                order.getPrice() / order.getQuantity(), // Цена за единицу
                order.getPrice(),
                order.getSize(),
                null // Код активации только для цифровых товаров
        );
        items.add(item);
        
        return new DetailedOrderDTO(
                order.getId(),
                order.getOrderNumber(),
                items,
                order.getTrackingNumber(),
                OrderStatusUtil.getStatusInRussian(order.getStatus()),
                order.getStatus().name(),
                order.getPrice(),
                order.getCreatedAt(),
                order.getPaymentMethod()
        );
    }

    /**
     * Маппинг цифрового заказа в DetailedOrderDTO
     */
    private DetailedOrderDTO mapToDetailedDTO(DigitalOrder digitalOrder) {
        List<OrderItemDTO> items = new ArrayList<>();
        
        // Преобразуем все элементы цифрового заказа
        for (DigitalOrderItem orderItem : digitalOrder.getItems()) {
            OrderItemDTO item = new OrderItemDTO(
                    orderItem.getDigitalProduct().getId(),
                    orderItem.getDigitalProduct().getName(),
                    "Цифровой продукт",
                    orderItem.getQuantity(),
                    orderItem.getPrice(),
                    orderItem.getTotalPrice(),
                    null, // Размер не применим к цифровым товарам
                    orderItem.getActivationCode()
            );
            items.add(item);
        }
        
        String status = digitalOrder.isPaid() ? "Выполнен" : "Ожидает оплаты";
        String statusCode = digitalOrder.isPaid() ? "COMPLETED" : "NEW";
        
        return new DetailedOrderDTO(
                digitalOrder.getId(),
                digitalOrder.getOrderNumber(),
                items,
                null, // Трек-номер для цифровых товаров не нужен
                status,
                statusCode,
                digitalOrder.getTotalPrice(),
                digitalOrder.getCreatedAt(),
                digitalOrder.getPaymentMethod()
        );
    }
}
