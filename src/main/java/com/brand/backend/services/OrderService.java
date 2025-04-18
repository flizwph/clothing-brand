package com.brand.backend.services;

import com.brand.backend.dtos.OrderDto;
import com.brand.backend.models.Order;
import com.brand.backend.models.Product;
import com.brand.backend.models.User;
import com.brand.backend.repositories.OrderRepository;
import com.brand.backend.repositories.ProductRepository;
import com.brand.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Order createOrder(OrderDto orderDto) {
        // Проверяем, существует ли продукт
        Product product = productRepository.findById(orderDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        // Проверяем, есть ли выбранный размер в наличии
        if (!isProductAvailable(product, orderDto.getSize())) {
            throw new RuntimeException("Выбранный размер недоступен");
        }

        // Проверяем, есть ли такой пользователь
        Optional<User> userOptional = userRepository.findByTelegramId(orderDto.getTelegramId());
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Пользователь не найден");
        }

        User user = userOptional.get();

        // Генерируем уникальный номер заказа
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8);

        // Создаем заказ
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setProduct(product);
        order.setQuantity(1);
        order.setSize(orderDto.getSize());
        order.setPrice(product.getPrice());
        order.setEmail(orderDto.getEmail());
        order.setFullName(orderDto.getFullName());
        order.setCountry(orderDto.getCountry());
        order.setAddress(orderDto.getAddress());
        order.setPostalCode(orderDto.getPostalCode());
        order.setPhoneNumber(orderDto.getPhoneNumber());
        order.setTelegramUsername(orderDto.getTelegramUsername());
        order.setCryptoAddress(orderDto.getCryptoAddress());
        order.setOrderComment(orderDto.getOrderComment());
        order.setPromoCode(orderDto.getPromoCode());
        order.setPaymentMethod("Crypto"); // Только крипта
        order.setCreatedAt(LocalDateTime.now());
        order.setUser(user);

        // Сохраняем заказ
        Order savedOrder = orderRepository.save(order);

        // Уменьшаем количество товара
        reduceProductQuantity(product, orderDto.getSize());

        return savedOrder;
    }

    private boolean isProductAvailable(Product product, String size) {
        return switch (size.toLowerCase()) {
            case "s" -> product.getAvailableQuantityS() > 0;
            case "m" -> product.getAvailableQuantityM() > 0;
            case "l" -> product.getAvailableQuantityL() > 0;
            default -> false;
        };
    }

    private void reduceProductQuantity(Product product, String size) {
        switch (size.toLowerCase()) {
            case "s" -> product.setAvailableQuantityS(product.getAvailableQuantityS() - 1);
            case "m" -> product.setAvailableQuantityM(product.getAvailableQuantityM() - 1);
            case "l" -> product.setAvailableQuantityL(product.getAvailableQuantityL() - 1);
        }
        productRepository.save(product);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

}
