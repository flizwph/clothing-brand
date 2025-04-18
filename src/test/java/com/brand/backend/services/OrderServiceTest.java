package com.brand.backend.services;

import com.brand.backend.application.order.service.OrderService;
import com.brand.backend.presentation.dto.request.OrderDto;
import com.brand.backend.presentation.dto.response.OrderResponseDto;
import com.brand.backend.domain.order.event.OrderEvent;
import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.order.model.OrderStatus;
import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.order.repository.OrderRepository;
import com.brand.backend.domain.product.repository.ProductRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private Order testOrder;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setTelegramId(123456789L);
        testUser.setCreatedAt(LocalDateTime.now());

        // Создаем тестовый товар
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(100.0);
        testProduct.setSizes(List.of("S", "M", "L"));
        testProduct.setAvailableQuantityS(10);
        testProduct.setAvailableQuantityM(10);
        testProduct.setAvailableQuantityL(10);

        // Создаем тестовый заказ
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-12345678");
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(1);
        testOrder.setSize("M");
        testOrder.setPrice(100.0);
        testOrder.setUser(testUser);
        testOrder.setEmail("test@example.com");
        testOrder.setPhoneNumber("+79123456789");
        testOrder.setFullName("Test User");
        testOrder.setCountry("Russia");
        testOrder.setAddress("Test Address");
        testOrder.setPostalCode("123456");
        testOrder.setPaymentMethod("card");
        testOrder.setStatus(OrderStatus.NEW);
        testOrder.setCreatedAt(LocalDateTime.now());

        // Создаем тестовый DTO заказа
        orderDto = new OrderDto();
        orderDto.setProductId(1L);
        orderDto.setQuantity(1);
        orderDto.setSize("M");
        orderDto.setEmail("test@example.com");
        orderDto.setPhoneNumber("+79123456789");
        orderDto.setFullName("Test User");
        orderDto.setCountry("Russia");
        orderDto.setAddress("Test Address");
        orderDto.setPostalCode("123456");
        orderDto.setPaymentMethod("card");
    }

    @Test
    void createOrder_Success() {
        // Подготавливаем моки
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Вызываем тестируемый метод
        OrderResponseDto result = orderService.createOrder("testuser", orderDto);

        // Проверяем результат
        assertNotNull(result);
        assertEquals("ORD-12345678", result.getOrderNumber());
        assertEquals("Test Product", result.getProductName());
        assertEquals("M", result.getSize());

        // Проверяем, что были вызваны нужные методы
        verify(userRepository).findByUsername("testuser");
        verify(productRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishEvent(any(OrderEvent.class));
    }

    @Test
    void getUserOrders_Success() {
        // Подготавливаем моки
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUser(testUser)).thenReturn(Arrays.asList(testOrder));

        // Вызываем тестируемый метод
        List<OrderResponseDto> result = orderService.getUserOrders("testuser");

        // Проверяем результат
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());

        // Проверяем, что были вызваны нужные методы
        verify(userRepository).findByUsername("testuser");
        verify(orderRepository).findByUser(testUser);
    }

    @Test
    void getOrderById_Success() {
        // Подготавливаем моки
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Вызываем тестируемый метод
        Optional<OrderResponseDto> result = orderService.getOrderById(1L);

        // Проверяем результат
        assertTrue(result.isPresent());
        assertEquals("ORD-12345678", result.get().getOrderNumber());

        // Проверяем, что были вызваны нужные методы
        verify(orderRepository).findById(1L);
    }

    @Test
    void cancelOrder_Success() {
        // Подготавливаем моки
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Вызываем тестируемый метод
        orderService.cancelOrder(1L, "testuser");

        // Проверяем, что были вызваны нужные методы
        verify(orderRepository).findById(1L);
        verify(orderRepository).delete(testOrder);
        verify(eventPublisher).publishEvent(any(OrderEvent.class));
    }

    @Test
    void updateOrderStatus_Success() {
        // Подготавливаем моки
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Вызываем тестируемый метод
        OrderResponseDto result = orderService.updateOrderStatus(1L, OrderStatus.PROCESSING);

        // Создаем аргумент-захватчик для проверки события
        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);

        // Проверяем результат
        assertNotNull(result);
        assertEquals(OrderStatus.PROCESSING, testOrder.getStatus());

        // Проверяем, что были вызваны нужные методы
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        // Проверяем тип события
        assertEquals(OrderEvent.OrderEventType.PAID, eventCaptor.getValue().getEventType());
    }

    @Test
    void getAllOrders_Success() {
        // Подготавливаем моки
        when(orderRepository.findAll()).thenReturn(Arrays.asList(testOrder));

        // Вызываем тестируемый метод
        List<OrderResponseDto> result = orderService.getAllOrders();

        // Проверяем результат
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());

        // Проверяем, что были вызваны нужные методы
        verify(orderRepository).findAll();
    }
} 