package com.brand.backend.services;

import com.brand.backend.dtos.PaymentDto;
import com.brand.backend.integration.payment.PaymentApiService;
import com.brand.backend.integration.payment.PaymentRequest;
import com.brand.backend.models.Order;
import com.brand.backend.models.Product;
import com.brand.backend.repositories.OrderRepository;
import com.brand.backend.repositories.ProductRepository;
import com.brand.backend.dtos.OrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentApiService paymentApiService;

    @Value("${payment.api.merchantId}")
    private String merchantId;

    @Transactional
    public Order createOrder(OrderDto orderDto) {
        Product product = productRepository.findById(orderDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        String size = orderDto.getSize();
        if (!product.getSizes().contains(size)) {
            throw new RuntimeException("Invalid size selected");
        }

        // Проверка доступного количества
        if (!isProductAvailable(product, size)) {
            throw new RuntimeException("Product is not available in the selected size");
        }

        // Обновление количества товара
        reduceProductQuantity(product, size);

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setProduct(product);
        order.setQuantity(1);
        order.setSize(size);
        // Цена берется непосредственно из продукта на момент создания заказа
        order.setPrice(product.getPrice());
        order.setEmail(orderDto.getEmail());
        order.setFullName(orderDto.getFullName());
        order.setCountry(orderDto.getCountry());
        order.setAddress(orderDto.getAddress());
        order.setPostalCode(orderDto.getPostalCode());
        order.setPhoneNumber(orderDto.getPhoneNumber());
        order.setTelegramUsername(orderDto.getTelegramUsername());
        order.setMetamaskAddress(orderDto.getMetamaskAddress());
        order.setOrderComment(orderDto.getOrderComment());
        order.setPromoCode(orderDto.getPromoCode());
        order.setPaymentMethod(orderDto.getPaymentMethod());
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // Создание и отправка запроса на оплату через PaymentApiService
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setMerchantId(merchantId);
        paymentRequest.setIdempotencyKey(generateOrderNumber());
        paymentRequest.setAmount(savedOrder.getPrice());
        paymentRequest.setCurrency("RUB");
        paymentRequest.setDescription("Оплата заказа №" + savedOrder.getOrderNumber());
        paymentRequest.setConfirmationType("redirect");
        paymentRequest.setReturnUrl("https://yourdomain.com/payment/result");

        paymentApiService.createPayment(paymentRequest);

        return savedOrder;
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    private boolean isProductAvailable(Product product, String size) {
        switch (size.toLowerCase()) {
            case "s":
                return product.getAvailableQuantityS() > 0;
            case "m":
                return product.getAvailableQuantityM() > 0;
            case "l":
                return product.getAvailableQuantityL() > 0;
            default:
                throw new RuntimeException("Invalid size");
        }
    }

    private void reduceProductQuantity(Product product, String size) {
        switch (size.toLowerCase()) {
            case "s":
                product.setAvailableQuantityS(product.getAvailableQuantityS() - 1);
                break;
            case "m":
                product.setAvailableQuantityM(product.getAvailableQuantityM() - 1);
                break;
            case "l":
                product.setAvailableQuantityL(product.getAvailableQuantityL() - 1);
                break;
            default:
                throw new RuntimeException("Invalid size");
        }
        productRepository.save(product);
    }
}
