package com.brand.backend.infrastructure.integration.telegram.admin.service;

import com.brand.backend.infrastructure.integration.telegram.admin.dto.OrderStatisticsDto;
import com.brand.backend.domain.nft.model.NFT;
import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.order.model.OrderStatus;
import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.nft.repository.NFTRepository;
import com.brand.backend.domain.order.repository.OrderRepository;
import com.brand.backend.domain.product.repository.ProductRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.application.nft.service.NFTService;
import com.brand.backend.application.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для обработки запросов от административного бота
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminBotService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NFTRepository nftRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final NFTService nftService;

    /**
     * Получает заказ по ID
     */
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }
    
    /**
     * Получает пользователя по ID
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
    
    /**
     * Получает заказы пользователя
     */
    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUser(user);
    }

    /**
     * Получает статистику по заказам
     */
    public OrderStatisticsDto getOrderStatistics() {
        List<Order> allOrders = orderRepository.findAll();
        
        // Рассчитываем основные метрики
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalCompletedOrders = 0;
        int totalCancelledOrders = 0;
        int newOrders = 0;
        int processingOrders = 0;
        int dispatchedOrders = 0;
        int completedOrders = 0;
        int cancelledOrders = 0;
        
        for (Order order : allOrders) {
            // Добавляем к общей выручке
            totalRevenue = totalRevenue.add(BigDecimal.valueOf(order.getPrice()));
            
            // Подсчитываем заказы по статусам
            switch (order.getStatus()) {
                case NEW -> newOrders++;
                case PROCESSING -> processingOrders++;
                case DISPATCHED -> dispatchedOrders++;
                case COMPLETED -> {
                    completedOrders++;
                    totalCompletedOrders++;
                }
                case CANCELLED -> {
                    cancelledOrders++;
                    totalCancelledOrders++;
                }
            }
        }
        
        // Временные рамки для статистики по периодам
        LocalDateTime today = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfMonth = today.withDayOfMonth(1);
        
        // Фильтруем заказы по периодам
        int ordersToday = (int) allOrders.stream().filter(order -> order.getCreatedAt().isAfter(today)).count();
        int ordersThisWeek = (int) allOrders.stream().filter(order -> order.getCreatedAt().isAfter(startOfWeek)).count();
        int ordersThisMonth = (int) allOrders.stream().filter(order -> order.getCreatedAt().isAfter(startOfMonth)).count();
        
        // Среднее значение заказа
        BigDecimal averageOrderValue = allOrders.isEmpty() 
            ? BigDecimal.ZERO 
            : totalRevenue.divide(BigDecimal.valueOf(allOrders.size()), 2, RoundingMode.HALF_UP);
        
        // Создаем и возвращаем DTO со статистикой
        return OrderStatisticsDto.builder()
                .totalOrders(allOrders.size())
                .totalRevenue(totalRevenue)
                .totalCompletedOrders(totalCompletedOrders)
                .totalCancelledOrders(totalCancelledOrders)
                .newOrders(newOrders)
                .processingOrders(processingOrders)
                .dispatchedOrders(dispatchedOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .ordersToday(ordersToday)
                .ordersThisWeek(ordersThisWeek)
                .ordersThisMonth(ordersThisMonth)
                .averageOrderValue(averageOrderValue)
                .build();
    }
    
    /**
     * Получает заказы по статусу
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        if (status == null) {
            return orderRepository.findAll();
        }
        
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    /**
     * Получает список пользователей
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Получает пользователя по имени пользователя
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }
    
    /**
     * Обновляет статус заказа
     */
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        try {
            orderService.updateOrderStatus(orderId, newStatus);
            return orderRepository.findById(orderId).orElse(null);
        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса заказа: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Получает заказы текущего месяца сгруппированные по дням
     */
    public Map<LocalDate, List<Order>> getOrdersByDays() {
        LocalDateTime startOfMonth = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIDNIGHT);
        
        return orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate()));
    }
    
    /**
     * Получает NFT для пользователя по имени пользователя
     */
    public List<NFT> getNFTsByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(nftRepository::findByUser)
                .orElse(List.of());
    }
    
    /**
     * Получает NFT для пользователя по объекту User
     */
    public List<NFT> getNFTsByUser(User user) {
        if (user == null) {
            return List.of();
        }
        return nftRepository.findByUser(user);
    }
    
    /**
     * Получает все NFT
     */
    public List<NFT> getAllNFTs() {
        return nftRepository.findAll();
    }
    
    /**
     * Получает нераскрытые NFT
     */
    public List<NFT> getUnrevealedNFTs() {
        return nftRepository.findAll().stream()
                .filter(nft -> !nft.isRevealed())
                .collect(Collectors.toList());
    }
    
    /**
     * Раскрывает NFT
     */
    public NFT revealNFT(Long nftId, String revealedUri) {
        try {
            nftService.revealNFT(nftId, revealedUri);
            return nftRepository.findById(nftId).orElse(null);
        } catch (Exception e) {
            log.error("Ошибка при раскрытии NFT: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Получает топ пользователей по количеству заказов
     */
    public List<User> getTopUsersByOrderCount(int limit) {
        Map<User, Long> userOrderCounts = orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(Order::getUser, Collectors.counting()));
        
        return userOrderCounts.entrySet().stream()
                .sorted(Map.Entry.<User, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Обновляет статус активности пользователя
     */
    public User updateUserActiveStatus(Long userId, boolean isActive) {
        User user = getUserById(userId);
        if (user == null) {
            return null;
        }
        
        user.setActive(isActive);
        return userRepository.save(user);
    }
    
    /**
     * Получает топ популярных товаров
     */
    public Map<Product, Integer> getTopProducts(int limit) {
        Map<Product, Integer> productCounts = new HashMap<>();
        
        for (Order order : orderRepository.findAll()) {
            Product product = order.getProduct();
            productCounts.put(product, productCounts.getOrDefault(product, 0) + order.getQuantity());
        }
        
        return productCounts.entrySet().stream()
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Поиск заказов по номеру заказа
     */
    public List<Order> searchOrdersByOrderNumber(String orderNumber) {
        return orderRepository.findAll().stream()
                .filter(order -> order.getOrderNumber().contains(orderNumber))
                .collect(Collectors.toList());
    }
    
    /**
     * Поиск заказов по телефону
     */
    public List<Order> searchOrdersByPhone(String phone) {
        return orderRepository.findAll().stream()
                .filter(order -> order.getPhoneNumber().contains(phone))
                .collect(Collectors.toList());
    }
    
    /**
     * Поиск заказов по email
     */
    public List<Order> searchOrdersByEmail(String email) {
        return orderRepository.findAll().stream()
                .filter(order -> order.getEmail().contains(email))
                .collect(Collectors.toList());
    }
} 