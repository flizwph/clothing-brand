package com.brand.backend.presentation.rest.controller.desktop;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.BatchOperationRequest;
import com.brand.backend.presentation.dto.request.desktop.CachePolicyRequest;
import com.brand.backend.presentation.dto.response.ApiResponse;
import com.brand.backend.presentation.dto.response.desktop.BatchOperationResponseDto;
import com.brand.backend.presentation.dto.response.desktop.CachePolicyResponseDto;
import com.brand.backend.application.desktop.service.DesktopBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Контроллер для пакетной обработки запросов и кэширования данных
 */
@RestController
@RequestMapping("/api/desktop")
@RequiredArgsConstructor
@Slf4j
public class DesktopBatchController {

    private final DesktopBatchService batchService;

    /**
     * Выполнение пакетных операций
     * 
     * @param request список операций для выполнения
     * @param user аутентифицированный пользователь
     * @return результаты выполнения операций
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<BatchOperationResponseDto>>> executeBatch(
            @Valid @RequestBody BatchOperationRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Выполнение пакетной операции для пользователя: {}, количество операций: {}", 
                user.getUsername(), request.getOperations().size());
        
        List<BatchOperationResponseDto> results = batchService.executeBatch(request, user);
        
        return ResponseEntity.ok(new ApiResponse<>(results));
    }

    /**
     * Настройка кэширования данных
     * 
     * @param request политики кэширования
     * @param user аутентифицированный пользователь
     * @return результат применения политик
     */
    @PostMapping("/cache/settings")
    public ResponseEntity<ApiResponse<CachePolicyResponseDto>> setCachePolicy(
            @Valid @RequestBody CachePolicyRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Настройка кэширования для пользователя: {}", user.getUsername());
        
        CachePolicyResponseDto result = batchService.setCachePolicy(request, user);
        
        return ResponseEntity.ok(new ApiResponse<>(result));
    }
    
    /**
     * Получение текущих настроек кэширования
     * 
     * @param user аутентифицированный пользователь
     * @return текущие настройки кэширования
     */
    @GetMapping("/cache/settings")
    public ResponseEntity<ApiResponse<CachePolicyResponseDto>> getCachePolicy(
            @AuthenticationPrincipal User user) {
        
        log.info("Получение настроек кэширования для пользователя: {}", user.getUsername());
        
        CachePolicyResponseDto result = batchService.getCachePolicy(user);
        
        return ResponseEntity.ok(new ApiResponse<>(result));
    }
} 