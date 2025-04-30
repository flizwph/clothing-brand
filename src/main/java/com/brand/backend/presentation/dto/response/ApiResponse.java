package com.brand.backend.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private T data;
    private boolean success = true;
    private String message;

    public ApiResponse(T data) {
        this.data = data;
    }

    public ApiResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
} 