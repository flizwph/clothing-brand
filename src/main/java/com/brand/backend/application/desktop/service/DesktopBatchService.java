package com.brand.backend.application.desktop.service;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.BatchOperationRequest;
import com.brand.backend.presentation.dto.request.desktop.CachePolicyRequest;
import com.brand.backend.presentation.dto.response.desktop.BatchOperationResponseDto;
import com.brand.backend.presentation.dto.response.desktop.CachePolicyResponseDto;

import java.util.List;

/**
 * Сервис для пакетной обработки запросов и кэширования данных в десктопном приложении
 */
public interface DesktopBatchService {

    /**
     * Выполняет пакетные операции
     * 
     * @param request список операций для выполнения
     * @param user пользователь
     * @return результаты выполнения операций
     */
    List<BatchOperationResponseDto> executeBatch(BatchOperationRequest request, User user);
    
    /**
     * Устанавливает политику кэширования
     * 
     * @param request политики кэширования
     * @param user пользователь
     * @return результат применения политик
     */
    CachePolicyResponseDto setCachePolicy(CachePolicyRequest request, User user);
    
    /**
     * Получает текущую политику кэширования
     * 
     * @param user пользователь
     * @return текущие настройки кэширования
     */
    CachePolicyResponseDto getCachePolicy(User user);
} 