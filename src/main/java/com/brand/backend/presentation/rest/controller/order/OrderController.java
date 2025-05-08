package com.brand.backend.presentation.rest.controller.order;

import com.brand.backend.presentation.dto.request.OrderDto;
import com.brand.backend.presentation.dto.response.OrderResponseDto;
import com.brand.backend.common.exeption.ResourceNotFoundException;
import com.brand.backend.application.order.service.OrderService;
import com.brand.backend.domain.user.model.User;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Отмена заказа", description = "Отменяет заказ текущего пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Заказ успешно отменен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            Authentication authentication) {
        
        String username;
        if (authentication.getPrincipal() instanceof User) {
            username = ((User) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        
        orderService.cancelOrder(id, username);
        return ResponseEntity.noContent().build();
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
