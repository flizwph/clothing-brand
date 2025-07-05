package com.brand.backend.presentation.rest.controller.subscription;

import com.brand.backend.application.subscription.service.SubscriptionService;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.response.SubscriptionInfoDTO;
import com.brand.backend.presentation.dto.response.SubscriptionPlanDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Подписки", description = "API для управления подписками")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "Получение текущей подписки", description = "Возвращает информацию о текущей подписке пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о подписке получена"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping("/current")
    public ResponseEntity<SubscriptionInfoDTO> getCurrentSubscription(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        
        SubscriptionInfoDTO subscription = subscriptionService.getDetailedSubscriptionInfo(user.getId());
        return ResponseEntity.ok(subscription);
    }

    @Operation(summary = "Моя подписка", description = "Возвращает информацию о подписке текущего пользователя (алиас для /current)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о подписке получена"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping("/my")
    public ResponseEntity<SubscriptionInfoDTO> getMySubscription(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.warn("Попытка получить подписку неавторизованным пользователем");
            return ResponseEntity.status(401).build();
        }
        
        log.debug("Запрос подписки пользователя: {}", user.getUsername());
        SubscriptionInfoDTO subscription = subscriptionService.getDetailedSubscriptionInfo(user.getId());
        return ResponseEntity.ok(subscription);
    }

    @Operation(summary = "Получение всех тарифных планов", description = "Возвращает список всех доступных тарифных планов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Тарифные планы получены")
    })
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanDTO>> getSubscriptionPlans() {
        List<SubscriptionPlanDTO> plans = subscriptionService.getAllActivePlans();
        return ResponseEntity.ok(plans);
    }

    @Operation(summary = "Продление подписки", description = "Продлевает текущую подписку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Подписка продлена"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Подписка не найдена")
    })
    @PostMapping("/renew")
    public ResponseEntity<Map<String, Object>> renewSubscription(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = subscriptionService.renewSubscription(user.getId());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Смена тарифного плана", description = "Изменяет тарифный план подписки")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "План изменен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "План не найден")
    })
    @PostMapping("/change-plan")
    public ResponseEntity<Map<String, Object>> changeSubscriptionPlan(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Новый уровень подписки") @RequestParam String level
    ) {
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = subscriptionService.changeSubscriptionPlan(user.getId(), level);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Управление автопродлением", description = "Включает/выключает автопродление подписки")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Автопродление обновлено"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @PostMapping("/auto-renewal")
    public ResponseEntity<Map<String, Object>> toggleAutoRenewal(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Включить/выключить автопродление") @RequestParam boolean enabled
    ) {
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = subscriptionService.toggleAutoRenewal(user.getId(), enabled);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Отмена подписки", description = "Отменяет текущую подписку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Подписка отменена"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelSubscription(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = subscriptionService.cancelSubscription(user.getId());
        return ResponseEntity.ok(result);
    }
} 