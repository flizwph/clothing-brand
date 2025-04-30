package com.brand.backend.presentation.dto.request.subscription;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class ActivateSubscriptionRequest {
    
    @NotBlank(message = "Код активации не может быть пустым")
    private String activationCode;
} 