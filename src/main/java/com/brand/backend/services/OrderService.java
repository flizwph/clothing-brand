package com.brand.backend.services;

import com.brand.backend.dtos.OrderDto;
import com.brand.backend.dtos.OrderResponseDto;
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
    public OrderResponseDto createOrder(OrderDto orderDto) {
        Product product = productRepository.findById(orderDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (!isProductAvailable(product, orderDto.getSize())) {
            throw new RuntimeException("Выбранный размер недоступен");
        }

        Optional<User> userOptional;

        if (orderDto.getUserId() != null) {
            userOptional = userRepository.findById(orderDto.getUserId());
        } else if (orderDto.getTelegramId() != null) {
            userOptional = userRepository.findByTelegramId(orderDto.getTelegramId());
        } else {
            throw new RuntimeException("Не передан ни userId, ни telegramId");
        }

        if (userOptional.isEmpty()) {
            throw new RuntimeException("Пользователь не найден");
        }

        User user = userOptional.get();

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8);

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

        Order savedOrder = orderRepository.save(order);

        reduceProductQuantity(product, orderDto.getSize());

        return mapToDto(savedOrder);
    }

    public Optional<OrderResponseDto> getOrderById(Long id) {
        return orderRepository.findById(id).map(this::mapToDto);
    }

    private boolean isProductAvailable(Product product, String size) {
        return switch (size.toLowerCase()) {
            case "M" -> product.getAvailableQuantityS() > 0;
            case "L" -> product.getAvailableQuantityM() > 0;
            case "XL" -> product.getAvailableQuantityL() > 0;
            default -> false;
        };
    }

    private void reduceProductQuantity(Product product, String size) {
        switch (size.toLowerCase()) {
            case "M" -> product.setAvailableQuantityS(product.getAvailableQuantityS() - 1);
            case "L" -> product.setAvailableQuantityM(product.getAvailableQuantityM() - 1);
            case "XL" -> product.setAvailableQuantityL(product.getAvailableQuantityL() - 1);
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
                order.getCreatedAt()
        );
    }

}
