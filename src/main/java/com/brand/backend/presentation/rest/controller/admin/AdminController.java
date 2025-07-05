package com.brand.backend.presentation.rest.controller.admin;

import com.brand.backend.presentation.dto.response.OrderResponseDto;
import com.brand.backend.domain.order.model.OrderStatus;
import com.brand.backend.application.nft.service.NFTService;
import com.brand.backend.application.order.service.OrderService;
import com.brand.backend.application.admin.cqrs.query.GetDashboardStatsQuery;
import com.brand.backend.application.admin.cqrs.result.DashboardStatsResult;
import com.brand.backend.application.auth.bus.QueryBus;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.application.auth.service.notification.TelegramNotificationService;
import com.brand.backend.application.payment.service.AdminNotificationService;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.presentation.dto.request.admin.AdminNotificationRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin General", description = "Общие админские эндпоинты")
public class AdminController {

    private final OrderService orderService;
    private final NFTService nftService;
    private final QueryBus queryBus;
    private final TelegramNotificationService telegramNotificationService;
    private final UserRepository userRepository;

    /**
     * Получение общей статистики (alias для /api/admin/dashboard/stats)
     */
    @GetMapping("/stats")
    @Operation(summary = "Общая статистика", description = "Получение основных метрик системы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика получена"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав доступа")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getGeneralStats(
            @AuthenticationPrincipal User admin) {
        
        log.info("📊 [ADMIN STATS] Запрос общей статистики от администратора: {}", admin.getUsername());
        
        try {
            GetDashboardStatsQuery query = GetDashboardStatsQuery.builder().build();
            DashboardStatsResult result = queryBus.dispatch(query);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            
            log.info("✅ [ADMIN STATS] Общая статистика успешно получена для: {}", admin.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("🔥 [ADMIN STATS] Ошибка получения общей статистики для {}: {}", 
                    admin.getUsername(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Ошибка получения статистики");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Отправка уведомлений пользователям
     */
    @PostMapping("/notifications")
    @Operation(summary = "Отправка уведомлений", description = "Массовая отправка уведомлений пользователям")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Уведомления отправлены"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав доступа")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendNotifications(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody AdminNotificationRequestDto request) {
        
        log.info("📢 [ADMIN NOTIFICATION] Отправка уведомлений от {}: тип={}, получателей={}", 
                admin.getUsername(), request.getType(), 
                request.getUserIds() != null ? request.getUserIds().size() : "ALL");
        
        try {
            int sentCount = 0;
            int failedCount = 0;
            
            // Определяем список получателей
            if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
                // Отправка конкретным пользователям
                for (Long userId : request.getUserIds()) {
                    try {
                        User user = userRepository.findById(userId).orElse(null);
                        if (user != null && user.getTelegramChatId() != null) {
                            boolean sent = telegramNotificationService.sendAdminMessage(
                                    user.getTelegramChatId(), 
                                    formatNotificationMessage(request, admin.getUsername())
                            );
                            if (sent) {
                                sentCount++;
                            } else {
                                failedCount++;
                            }
                        } else {
                            failedCount++;
                        }
                    } catch (Exception e) {
                        log.error("Ошибка отправки уведомления пользователю {}: {}", userId, e.getMessage());
                        failedCount++;
                    }
                }
            } else {
                // Массовая отправка всем активным пользователям с Telegram
                var activeUsers = userRepository.findByIsActiveTrueAndTelegramChatIdIsNotNull();
                for (User user : activeUsers) {
                    try {
                        boolean sent = telegramNotificationService.sendAdminMessage(
                                user.getTelegramChatId(), 
                                formatNotificationMessage(request, admin.getUsername())
                        );
                        if (sent) {
                            sentCount++;
                        } else {
                            failedCount++;
                        }
                    } catch (Exception e) {
                        log.error("Ошибка отправки уведомления пользователю {}: {}", user.getUsername(), e.getMessage());
                        failedCount++;
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Уведомления отправлены");
            response.put("sentCount", sentCount);
            response.put("failedCount", failedCount);
            response.put("totalAttempts", sentCount + failedCount);
            
            log.info("✅ [ADMIN NOTIFICATION] Уведомления отправлены админом {}: успешно={}, ошибок={}", 
                    admin.getUsername(), sentCount, failedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("🔥 [ADMIN NOTIFICATION] Ошибка отправки уведомлений админом {}: {}", 
                    admin.getUsername(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Ошибка отправки уведомлений");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Форматирует сообщение уведомления
     */
    private String formatNotificationMessage(AdminNotificationRequestDto request, String adminUsername) {
        StringBuilder message = new StringBuilder();
        
        // Добавляем эмодзи в зависимости от типа
        String emoji = switch (request.getType().toUpperCase()) {
            case "INFO" -> "ℹ️";
            case "WARNING" -> "⚠️";
            case "ERROR" -> "🚨";
            case "SUCCESS" -> "✅";
            case "MAINTENANCE" -> "🔧";
            case "ANNOUNCEMENT" -> "📢";
            default -> "📝";
        };
        
        message.append(emoji).append(" **УВЕДОМЛЕНИЕ ОТ АДМИНИСТРАЦИИ**\n\n");
        message.append("**Тип:** ").append(request.getType().toUpperCase()).append("\n");
        message.append("**Сообщение:**\n").append(request.getMessage()).append("\n\n");
        
        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            message.insert(message.indexOf("**Тип:**"), "**" + request.getTitle() + "**\n\n");
        }
        
        message.append("_Отправлено администратором: ").append(adminUsername).append("_");
        
        return message.toString();
    }

    // Метод getAllOrders перенесён в AdminOrderController

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> statusRequest) {
        
        String statusString = statusRequest.get("status");
        if (statusString == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            OrderStatus newStatus = OrderStatus.valueOf(statusString.toUpperCase());
            OrderResponseDto updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/nft/{nftId}/reveal")
    public ResponseEntity<?> revealNFT(
            @PathVariable Long nftId,
            @RequestBody Map<String, String> revealRequest) {
        
        String revealedUri = revealRequest.get("revealedUri");
        if (revealedUri == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            nftService.revealNFT(nftId, revealedUri);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // Метод getAllUsers перенесён в AdminUserController
} 