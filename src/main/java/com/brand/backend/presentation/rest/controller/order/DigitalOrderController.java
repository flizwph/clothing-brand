package com.brand.backend.presentation.rest.controller.order;

import com.brand.backend.application.order.service.DigitalOrderService;
import com.brand.backend.common.exeption.ResourceNotFoundException;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.DigitalOrderDto;
import com.brand.backend.presentation.dto.response.DigitalOrderItemResponseDto;
import com.brand.backend.presentation.dto.response.DigitalOrderResponseDto;
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
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/digital-orders")
@RequiredArgsConstructor
@Tag(name = "Цифровые заказы", description = "API для управления цифровыми заказами")
public class DigitalOrderController {

    private final DigitalOrderService digitalOrderService;

    @Operation(summary = "Создание нового цифрового заказа", description = "Создает новый цифровой заказ для текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Заказ успешно создан",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DigitalOrderResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные заказа"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    @PostMapping
    public ResponseEntity<DigitalOrderResponseDto> createDigitalOrder(
            @Parameter(description = "Данные нового цифрового заказа") @Valid @RequestBody DigitalOrderDto orderDto,
            Authentication authentication) {
        
        String username;
        if (authentication.getPrincipal() instanceof User) {
            username = ((User) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        
        log.info("Получен запрос на создание цифрового заказа от пользователя: {}", username);
        
        try {
            DigitalOrderResponseDto createdOrder = digitalOrderService.createOrder(username, orderDto);
            log.info("Цифровой заказ успешно создан для пользователя: {}", username);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (UsernameNotFoundException e) {
            log.error("Ошибка при создании цифрового заказа: пользователь не найден: {}", username, e);
            throw new ResourceNotFoundException("Пользователь", "username", username);
        } catch (Exception e) {
            log.error("Ошибка при создании цифрового заказа для пользователя {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Получение цифрового заказа по ID", description = "Возвращает данные цифрового заказа по его идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DigitalOrderResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DigitalOrderResponseDto> getDigitalOrderById(
            @Parameter(description = "ID заказа") @PathVariable Long id) {
        
        return digitalOrderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Цифровой заказ", "id", id));
    }

    @Operation(summary = "Получение всех цифровых заказов пользователя", description = "Возвращает список всех цифровых заказов текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список заказов успешно получен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping
    public ResponseEntity<List<DigitalOrderResponseDto>> getUserDigitalOrders(Authentication authentication) {
        String username;
        if (authentication.getPrincipal() instanceof User) {
            username = ((User) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        
        List<DigitalOrderResponseDto> orders = digitalOrderService.getUserOrders(username);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Получение статуса цифрового заказа", description = "Возвращает информацию о статусе цифрового заказа по его идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус заказа получен успешно"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getDigitalOrderStatus(
            @Parameter(description = "ID заказа") @PathVariable Long id) {
        
        return digitalOrderService.getOrderById(id)
                .map(order -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("orderNumber", order.getOrderNumber());
                    response.put("isPaid", order.isPaid());
                    response.put("paymentMethod", order.getPaymentMethod());
                    response.put("createdAt", order.getCreatedAt());
                    response.put("paymentDate", order.getPaymentDate());
                    return ResponseEntity.ok(response);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Цифровой заказ", "id", id));
    }

    @Operation(summary = "Активация цифрового продукта", description = "Активирует цифровой продукт по его ID в заказе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Продукт успешно активирован"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Продукт не найден")
    })
    @PostMapping("/items/{itemId}/activate")
    public ResponseEntity<DigitalOrderItemResponseDto> activateDigitalProduct(
            @Parameter(description = "ID позиции заказа") @PathVariable Long itemId,
            Authentication authentication) {
        
        String username;
        if (authentication.getPrincipal() instanceof User) {
            username = ((User) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        
        try {
            DigitalOrderItemResponseDto activatedItem = digitalOrderService.activateOrderItem(username, itemId);
            return ResponseEntity.ok(activatedItem);
        } catch (Exception e) {
            log.error("Ошибка при активации цифрового продукта: {}", e.getMessage(), e);
            throw e;
        }
    }
} 