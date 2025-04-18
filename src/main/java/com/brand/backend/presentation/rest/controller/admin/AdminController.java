package com.brand.backend.presentation.rest.controller.admin;

import com.brand.backend.presentation.dto.response.OrderResponseDto;
import com.brand.backend.domain.order.model.OrderStatus;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.application.nft.service.NFTService;
import com.brand.backend.application.order.service.OrderService;
import com.brand.backend.application.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrderService orderService;
    private final UserService userService;
    private final NFTService nftService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        List<OrderResponseDto> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

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
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        
        // Для безопасности не возвращаем хеши паролей
        users.forEach(user -> user.setPasswordHash("[PROTECTED]"));
        
        return ResponseEntity.ok(users);
    }
} 