package com.brand.backend.presentation.rest.controller.order;

import com.brand.backend.presentation.dto.request.OrderDto;
import com.brand.backend.presentation.dto.request.UpdateOrderDto;
import com.brand.backend.presentation.dto.response.DetailedOrderDTO;
import com.brand.backend.presentation.dto.response.OrderResponseDto;
import com.brand.backend.common.exception.ResourceNotFoundException;
import com.brand.backend.application.order.service.OrderService;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Заказы", description = "API для управления заказами")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Создание нового заказа", description = "Создает новый заказ для текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Заказ успешно создан",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные заказа"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @Parameter(description = "Данные нового заказа") @Valid @RequestBody OrderDto orderDto,
            Authentication authentication) {
        
        String username;
        if (authentication.getPrincipal() instanceof User) {
            username = ((User) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        
        log.info("Получен запрос на создание заказа от пользователя: {}", username);
        
        try {
            OrderResponseDto createdOrder = orderService.createOrder(username, orderDto);
            log.info("Заказ успешно создан для пользователя: {}", username);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (UsernameNotFoundException e) {
            log.error("Ошибка при создании заказа: пользователь не найден: {}", username, e);
            throw new ResourceNotFoundException("Пользователь", "username", username);
        } catch (Exception e) {
            log.error("Ошибка при создании заказа для пользователя {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Получение заказа по ID", description = "Возвращает данные заказа по его идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @Parameter(description = "ID заказа") @PathVariable Long id) {
        
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ", "id", id));
    }

    @Operation(summary = "Получение всех заказов пользователя", description = "Возвращает список всех заказов текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список заказов успешно получен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getUserOrders(Authentication authentication) {
        String username;
        if (authentication.getPrincipal() instanceof User) {
            username = ((User) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        
        List<OrderResponseDto> orders = orderService.getUserOrders(username);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Отмена заказа", description = "Отменяет заказ текущего пользователя по ID (доступно в течение 24 часов)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ успешно отменен"),
            @ApiResponse(responseCode = "400", description = "Срок отмены истек или заказ нельзя отменить"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            Authentication authentication) {
        
        String username;
        if (authentication.getPrincipal() instanceof User) {
            username = ((User) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        
        try {
        orderService.cancelOrder(id, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Заказ успешно отменен");
            response.put("orderId", id);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @Operation(summary = "Редактирование заказа", description = "Редактирует данные заказа (доступно в течение 24 часов, сумма не изменяется)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ успешно обновлен"),
            @ApiResponse(responseCode = "400", description = "Срок редактирования истек или некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDto> updateOrder(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @Parameter(description = "Новые данные заказа") @Valid @RequestBody UpdateOrderDto updateDto,
            Authentication authentication) {
        
        String username;
        if (authentication.getPrincipal() instanceof User) {
            username = ((User) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        
        try {
            OrderResponseDto updatedOrder = orderService.updateOrder(id, username, updateDto);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            log.error("Ошибка при обновлении заказа {}: {}", id, e.getMessage());
            throw e;
        }
    }
    
    @Operation(summary = "Получение статуса заказа", description = "Возвращает информацию о статусе заказа по его идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус заказа получен успешно"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getOrderStatus(
            @Parameter(description = "ID заказа") @PathVariable Long id) {
        
        return orderService.getOrderById(id)
                .map(order -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("orderNumber", order.getOrderNumber());
                    response.put("status", order.getStatus());
                    response.put("createdAt", order.getCreatedAt());
                    return ResponseEntity.ok(response);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Заказ", "id", id));
    }
    
    @Operation(summary = "Получение активных заказов", description = "Возвращает список всех активных заказов пользователя с детальной информацией")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список активных заказов успешно получен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping("/active")
    public ResponseEntity<List<DetailedOrderDTO>> getActiveOrders(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        
        List<DetailedOrderDTO> activeOrders = orderService.getActiveOrdersDetailed(user.getId());
        return ResponseEntity.ok(activeOrders);
    }

    @Operation(summary = "Получение истории заказов", description = "Возвращает список всех завершенных заказов пользователя с детальной информацией")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "История заказов успешно получена"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping("/history")
    public ResponseEntity<List<DetailedOrderDTO>> getOrderHistory(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        
        List<DetailedOrderDTO> orderHistory = orderService.getOrderHistoryDetailed(user.getId());
        return ResponseEntity.ok(orderHistory);
    }

    @Operation(summary = "Трекинг доставки заказов", description = "Возвращает информацию о доставке всех активных заказов пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о трекинге получена"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping("/status-tracking")
    public ResponseEntity<Map<String, Object>> getOrderTracking(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности для трекинга");
            return ResponseEntity.notFound().build();
        }
        
        log.info("📦 [ORDER TRACKING] Запрос трекинга заказов от пользователя: {}", user.getUsername());
        
        try {
            List<DetailedOrderDTO> activeOrders = orderService.getActiveOrdersDetailed(user.getId());
            
            Map<String, Object> trackingInfo = new HashMap<>();
            trackingInfo.put("success", true);
            trackingInfo.put("totalActiveOrders", activeOrders.size());
            trackingInfo.put("orders", activeOrders.stream().map(order -> {
                Map<String, Object> orderTracking = new HashMap<>();
                orderTracking.put("orderId", order.getId());
                orderTracking.put("orderNumber", order.getOrderNumber());
                orderTracking.put("status", order.getStatus());
                orderTracking.put("statusCode", order.getStatusCode());
                orderTracking.put("trackingNumber", order.getTrackingNumber());
                orderTracking.put("createdAt", order.getCreatedAt());
                orderTracking.put("totalPrice", order.getTotalPrice());
                orderTracking.put("paymentMethod", order.getPaymentMethod());
                
                // Добавляем информацию о прогрессе доставки
                String progress = getDeliveryProgress(order.getStatusCode());
                orderTracking.put("deliveryProgress", progress);
                orderTracking.put("estimatedDelivery", getEstimatedDelivery(order.getStatusCode()));
                orderTracking.put("canTrack", order.getTrackingNumber() != null && !order.getTrackingNumber().isEmpty());
                
                return orderTracking;
            }).toList());
            
            log.info("✅ [ORDER TRACKING] Информация о трекинге передана для {} заказов пользователя: {}", 
                    activeOrders.size(), user.getUsername());
            
            return ResponseEntity.ok(trackingInfo);
            
        } catch (Exception e) {
            log.error("🔥 [ORDER TRACKING] Ошибка получения трекинга для пользователя {}: {}", 
                    user.getUsername(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Ошибка получения информации о трекинге");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(summary = "Завершение заказа", description = "Завершает заказ (доступно только администраторам и продавцам)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ успешно завершен"),
            @ApiResponse(responseCode = "400", description = "Заказ нельзя завершить"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав доступа"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public ResponseEntity<Map<String, Object>> completeOrder(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        
        log.info("🏁 [ORDER COMPLETE] Запрос на завершение заказа {} от: {}", id, admin.getUsername());
        
        try {
            OrderResponseDto completedOrder = orderService.updateOrderStatus(id, OrderStatus.COMPLETED);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Заказ успешно завершен");
            response.put("orderId", id);
            response.put("orderNumber", completedOrder.getOrderNumber());
            response.put("status", completedOrder.getStatus());
            response.put("completedBy", admin.getUsername());
            response.put("completedAt", java.time.LocalDateTime.now());
            
            log.info("✅ [ORDER COMPLETE] Заказ {} успешно завершен администратором: {}", 
                    completedOrder.getOrderNumber(), admin.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("🔥 [ORDER COMPLETE] Ошибка завершения заказа {} админом {}: {}", 
                    id, admin.getUsername(), e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Ошибка при завершении заказа");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Получает прогресс доставки на основе статуса заказа
     */
    private String getDeliveryProgress(String statusCode) {
        return switch (statusCode) {
            case "NEW" -> "Заказ принят";
            case "PROCESSING" -> "Заказ обрабатывается";
            case "DISPATCHED" -> "Заказ отправлен";
            case "COMPLETED" -> "Заказ доставлен";
            case "CANCELLED" -> "Заказ отменен";
            default -> "Статус неизвестен";
        };
    }

    /**
     * Получает примерное время доставки на основе статуса
     */
    private String getEstimatedDelivery(String statusCode) {
        return switch (statusCode) {
            case "NEW" -> "3-5 рабочих дней после обработки";
            case "PROCESSING" -> "3-5 рабочих дней после отправки";
            case "DISPATCHED" -> "1-3 рабочих дня";
            case "COMPLETED" -> "Доставлен";
            case "CANCELLED" -> "Отменен";
            default -> "Неизвестно";
        };
    }
    
    /**
     * Сообщает об ошибке, если клиент отправляет item JSON
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        
        log.warn("Получен некорректный JSON: {}", ex.getMessage());
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "Некорректный формат JSON");
        
        if (ex.getMessage().contains("items")) {
            response.put("message", "Для создания заказа с несколькими товарами используйте API: /api/digital-orders");
            response.put("hint", "API /api/orders предназначен только для заказов с одним товаром");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        response.put("message", "Проверьте формат отправляемых данных");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
