package com.brand.backend.application.desktop.service.impl;

import com.brand.backend.application.desktop.service.DesktopBatchService;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.desktop.BatchOperationRequest;
import com.brand.backend.presentation.dto.request.desktop.CachePolicyRequest;
import com.brand.backend.presentation.dto.response.desktop.BatchOperationResponseDto;
import com.brand.backend.presentation.dto.response.desktop.CachePolicyResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для пакетной обработки запросов и кэширования данных в десктопном приложении
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DesktopBatchServiceImpl implements DesktopBatchService {

    /**
     * Выполняет пакетные операции
     * 
     * @param request список операций для выполнения
     * @param user пользователь
     * @return результаты выполнения операций
     */
    @Override
    public List<BatchOperationResponseDto> executeBatch(BatchOperationRequest request, User user) {
        log.info("Выполнение пакетной операции для пользователя: {}, количество операций: {}", 
                user.getUsername(), request.getOperations().size());
        
        // Сортируем операции по порядку и зависимостям
        List<BatchOperationRequest.BatchOperation> sortedOperations = sortOperations(request.getOperations());
        
        // Результаты выполнения операций
        List<BatchOperationResponseDto> results = new ArrayList<>();
        Map<String, BatchOperationResponseDto> resultsMap = new HashMap<>();
        
        // Выполняем операции последовательно
        for (BatchOperationRequest.BatchOperation operation : sortedOperations) {
            // Проверяем зависимости
            boolean dependenciesFulfilled = checkDependencies(operation, resultsMap);
            
            if (!dependenciesFulfilled) {
                // Если зависимости не выполнены, создаем ответ с ошибкой
                BatchOperationResponseDto errorResult = createErrorResponse(
                        operation.getId(), 
                        "DEPENDENCY_FAILED", 
                        "Не выполнены зависимые операции"
                );
                results.add(errorResult);
                resultsMap.put(operation.getId(), errorResult);
                
                if (!request.isContinueOnError()) {
                    // Если не продолжаем при ошибке, завершаем выполнение
                    break;
                }
                
                continue;
            }
            
            // Выполняем операцию
            BatchOperationResponseDto result = executeOperation(operation, user);
            results.add(result);
            resultsMap.put(operation.getId(), result);
            
            if (!result.isSuccess() && !request.isContinueOnError()) {
                // Если операция не успешна и не продолжаем при ошибке, завершаем выполнение
                break;
            }
        }
        
        return results;
    }

    /**
     * Устанавливает политику кэширования
     * 
     * @param request политики кэширования
     * @param user пользователь
     * @return результат применения политик
     */
    @Override
    public CachePolicyResponseDto setCachePolicy(CachePolicyRequest request, User user) {
        log.info("Настройка кэширования для пользователя: {}", user.getUsername());
        
        // TODO: Реализовать сохранение политики кэширования в базе данных
        
        // Создаем демонстрационный ответ
        return createCachePolicyResponse(request, user);
    }
    
    /**
     * Получает текущую политику кэширования
     * 
     * @param user пользователь
     * @return текущие настройки кэширования
     */
    @Override
    public CachePolicyResponseDto getCachePolicy(User user) {
        log.info("Получение настроек кэширования для пользователя: {}", user.getUsername());
        
        // TODO: Реализовать получение политики кэширования из базы данных
        
        // Создаем демонстрационные данные по умолчанию
        CachePolicyRequest defaultRequest = CachePolicyRequest.builder()
                .globalSettings(CachePolicyRequest.GlobalCacheSettings.builder()
                        .enabled(true)
                        .maxCacheSize(100)
                        .defaultTtl(60)
                        .cachePoorConnections(true)
                        .autoCleanup(true)
                        .build())
                .resourceSettings(Map.of(
                        "products", CachePolicyRequest.ResourceCacheSettings.builder()
                                .enabled(true)
                                .ttl(30)
                                .priority(8)
                                .preload(true)
                                .build(),
                        "settings", CachePolicyRequest.ResourceCacheSettings.builder()
                                .enabled(true)
                                .ttl(120)
                                .priority(10)
                                .preload(false)
                                .build()
                ))
                .build();
        
        // Создаем ответ
        return createCachePolicyResponse(defaultRequest, user);
    }
    
    /**
     * Сортирует операции по порядку и зависимостям
     * 
     * @param operations список операций
     * @return отсортированный список операций
     */
    private List<BatchOperationRequest.BatchOperation> sortOperations(List<BatchOperationRequest.BatchOperation> operations) {
        // Сначала сортируем по order (если задан)
        List<BatchOperationRequest.BatchOperation> sortedOperations = operations.stream()
                .sorted((op1, op2) -> {
                    // Если order не задан, считаем его равным Integer.MAX_VALUE
                    Integer order1 = op1.getOrder() != null ? op1.getOrder() : Integer.MAX_VALUE;
                    Integer order2 = op2.getOrder() != null ? op2.getOrder() : Integer.MAX_VALUE;
                    return order1.compareTo(order2);
                })
                .collect(Collectors.toList());
        
        // TODO: Реализовать топологическую сортировку по зависимостям
        
        return sortedOperations;
    }
    
    /**
     * Проверяет выполнение зависимостей
     * 
     * @param operation операция
     * @param resultsMap карта результатов выполнения операций
     * @return true, если все зависимости выполнены успешно
     */
    private boolean checkDependencies(BatchOperationRequest.BatchOperation operation, Map<String, BatchOperationResponseDto> resultsMap) {
        if (operation.getDependsOn() == null || operation.getDependsOn().isEmpty()) {
            return true;
        }
        
        for (String dependencyId : operation.getDependsOn()) {
            BatchOperationResponseDto dependencyResult = resultsMap.get(dependencyId);
            if (dependencyResult == null || !dependencyResult.isSuccess()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Выполняет операцию
     * 
     * @param operation операция
     * @param user пользователь
     * @return результат выполнения операции
     */
    private BatchOperationResponseDto executeOperation(BatchOperationRequest.BatchOperation operation, User user) {
        log.info("Выполнение операции: {}, тип: {}, метод: {}, путь: {}", 
                operation.getId(), operation.getType(), operation.getMethod(), operation.getPath());
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // Эмулируем выполнение операции с некоторой задержкой
        try {
            // Случайная задержка от 100 до 500 мс для эмуляции выполнения
            TimeUnit.MILLISECONDS.sleep(new Random().nextInt(400) + 100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Эмулируем случайный результат (в большинстве случаев успех)
        boolean success = new Random().nextInt(10) < 9;
        
        LocalDateTime endTime = LocalDateTime.now();
        long executionTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
        
        // Создаем ответ
        if (success) {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("result", "success");
            responseData.put("timestamp", System.currentTimeMillis());
            
            return BatchOperationResponseDto.builder()
                    .id(operation.getId())
                    .success(true)
                    .statusCode(200)
                    .data(responseData)
                    .startTime(startTime)
                    .endTime(endTime)
                    .executionTimeMs(executionTimeMs)
                    .build();
        } else {
            return createErrorResponse(
                    operation.getId(), 
                    "OPERATION_FAILED", 
                    "Ошибка выполнения операции"
            );
        }
    }
    
    /**
     * Создает ответ с ошибкой
     * 
     * @param operationId идентификатор операции
     * @param errorCode код ошибки
     * @param errorMessage сообщение об ошибке
     * @return ответ с ошибкой
     */
    private BatchOperationResponseDto createErrorResponse(String operationId, String errorCode, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        
        return BatchOperationResponseDto.builder()
                .id(operationId)
                .success(false)
                .statusCode(400)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .data(new HashMap<>())
                .startTime(now)
                .endTime(now)
                .executionTimeMs(0L)
                .build();
    }
    
    /**
     * Создает ответ с настройками кэширования
     * 
     * @param request запрос с настройками кэширования
     * @param user пользователь
     * @return ответ с настройками кэширования
     */
    private CachePolicyResponseDto createCachePolicyResponse(CachePolicyRequest request, User user) {
        // Преобразуем настройки из запроса в формат ответа
        CachePolicyResponseDto.GlobalSettings globalSettings = CachePolicyResponseDto.GlobalSettings.builder()
                .enabled(request.getGlobalSettings().isEnabled())
                .maxCacheSize(request.getGlobalSettings().getMaxCacheSize())
                .defaultTtl(request.getGlobalSettings().getDefaultTtl())
                .cachePoorConnections(request.getGlobalSettings().isCachePoorConnections())
                .autoCleanup(request.getGlobalSettings().isAutoCleanup())
                .build();
        
        Map<String, CachePolicyResponseDto.ResourceSettings> resourceSettings = new HashMap<>();
        
        if (request.getResourceSettings() != null) {
            request.getResourceSettings().forEach((key, value) -> 
                resourceSettings.put(key, CachePolicyResponseDto.ResourceSettings.builder()
                        .enabled(value.isEnabled())
                        .ttl(value.getTtl())
                        .priority(value.getPriority())
                        .preload(value.isPreload())
                        .build())
            );
        }
        
        // Создаем статистику использования кэша (демонстрационные данные)
        Random random = new Random();
        double usedSpace = random.nextDouble() * 50; // от 0 до 50 МБ
        double maxCacheSize = request.getGlobalSettings().getMaxCacheSize();
        
        CachePolicyResponseDto.CacheStats cacheStats = CachePolicyResponseDto.CacheStats.builder()
                .usedSpace(usedSpace)
                .availableSpace(maxCacheSize - usedSpace)
                .usagePercent((usedSpace / maxCacheSize) * 100)
                .itemCount(random.nextInt(500) + 100) // от 100 до 600 элементов
                .resourceStats(createResourceStats(resourceSettings.keySet()))
                .build();
        
        return CachePolicyResponseDto.builder()
                .settings(CachePolicyResponseDto.CacheSettings.builder()
                        .global(globalSettings)
                        .resources(resourceSettings)
                        .build())
                .stats(cacheStats)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
    
    /**
     * Создает статистику использования кэша для ресурсов
     * 
     * @param resourceKeys набор ключей ресурсов
     * @return статистика использования кэша для ресурсов
     */
    private Map<String, CachePolicyResponseDto.ResourceStats> createResourceStats(Set<String> resourceKeys) {
        Map<String, CachePolicyResponseDto.ResourceStats> resourceStats = new HashMap<>();
        Random random = new Random();
        
        for (String key : resourceKeys) {
            resourceStats.put(key, CachePolicyResponseDto.ResourceStats.builder()
                    .usedSpace(random.nextDouble() * 10) // от 0 до 10 МБ
                    .itemCount(random.nextInt(100) + 10) // от 10 до 110 элементов
                    .hitRate(random.nextDouble() * 100) // от 0 до 100%
                    .lastUpdated(LocalDateTime.now().minusMinutes(random.nextInt(60)))
                    .build());
        }
        
        return resourceStats;
    }
} 