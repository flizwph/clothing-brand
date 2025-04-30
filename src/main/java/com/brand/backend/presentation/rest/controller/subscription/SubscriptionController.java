package com.brand.backend.presentation.rest.controller.subscription;

import com.brand.backend.application.subscription.service.SubscriptionService;
import com.brand.backend.domain.subscription.model.PurchasePlatform;
import com.brand.backend.domain.subscription.model.Subscription;
import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import com.brand.backend.presentation.dto.request.subscription.ActivateSubscriptionRequest;
import com.brand.backend.presentation.dto.request.subscription.CreateSubscriptionRequest;
import com.brand.backend.presentation.dto.response.subscription.SubscriptionResponse;
import com.brand.backend.presentation.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(@RequestBody CreateSubscriptionRequest request) {
        Subscription subscription = subscriptionService.createSubscription(
                request.getUserId(),
                request.getLevel(),
                request.getDurationInDays(),
                request.getPlatform()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(mapToResponse(subscription)));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> activateSubscription(@RequestBody ActivateSubscriptionRequest request) {
        Subscription subscription = subscriptionService.activateSubscription(request.getActivationCode());
        return ResponseEntity.ok(new ApiResponse<>(mapToResponse(subscription)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getUserSubscriptions(@PathVariable Long userId) {
        List<Subscription> subscriptions = subscriptionService.getUserActiveSubscriptions(userId);
        List<SubscriptionResponse> responses = subscriptions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(new ApiResponse<>(responses));
    }

    @GetMapping("/check/{userId}/{level}")
    public ResponseEntity<ApiResponse<Boolean>> checkSubscription(
            @PathVariable Long userId,
            @PathVariable SubscriptionLevel level) {
        boolean isActive = subscriptionService.isSubscriptionActive(userId, level);
        return ResponseEntity.ok(new ApiResponse<>(isActive));
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .activationCode(subscription.getActivationCode())
                .subscriptionLevel(subscription.getSubscriptionLevel())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .isActive(subscription.isActive())
                .purchasePlatform(subscription.getPurchasePlatform())
                .build();
    }
} 