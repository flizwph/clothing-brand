package com.brand.backend.application.auth.mediator.impl;

import com.brand.backend.application.auth.mediator.Mediator;
import com.brand.backend.application.auth.mediator.Request;
import com.brand.backend.application.auth.mediator.RequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Реализация медиатора на основе Spring Context
 * Автоматически обнаруживает и регистрирует всех обработчиков запросов
 */
@Slf4j
@Component
public class SpringMediator implements Mediator {

    private final Map<Class<? extends Request<?>>, RequestHandler<?, ?>> handlers = new HashMap<>();
    
    /**
     * Конструктор регистрирует всех обработчиков запросов из Spring-контекста
     */
    public SpringMediator(ApplicationContext applicationContext) {
        Map<String, RequestHandler> handlerBeans = applicationContext.getBeansOfType(RequestHandler.class);
        
        for (RequestHandler<?, ?> handler : handlerBeans.values()) {
            registerHandler(handler);
        }
        
        log.info("Зарегистрировано {} обработчиков запросов в медиаторе", handlers.size());
    }
    
    /**
     * Регистрирует обработчик запросов
     */
    @SuppressWarnings("unchecked")
    private <T, R extends Request<T>> void registerHandler(RequestHandler<T, R> handler) {
        try {
            Class<?>[] typeArguments = GenericTypeResolver.resolveTypeArguments(
                    handler.getClass(), RequestHandler.class);
            
            if (typeArguments != null && typeArguments.length >= 2) {
                Class<R> requestType = (Class<R>) typeArguments[1];
                handlers.put(requestType, handler);
                log.debug("Зарегистрирован обработчик для запроса: {}", requestType.getSimpleName());
            }
        } catch (Exception e) {
            log.error("Не удалось зарегистрировать обработчик: {}", handler.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Отправляет запрос соответствующему обработчику
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T, R extends Request<T>> T send(R request) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        Class<?> requestClass = request.getClass();
        log.debug("[{}] Обработка запроса: {}", requestId, requestClass.getSimpleName());
        
        RequestHandler<T, R> handler = (RequestHandler<T, R>) handlers.get(requestClass);
        
        if (handler == null) {
            log.error("[{}] Обработчик не найден для запроса: {}", requestId, requestClass.getSimpleName());
            throw new IllegalArgumentException("No handler registered for request: " + requestClass.getSimpleName());
        }
        
        try {
            T result = handler.handle(request);
            log.debug("[{}] Запрос успешно обработан: {}", requestId, requestClass.getSimpleName());
            return result;
        } catch (Exception e) {
            log.error("[{}] Ошибка при обработке запроса {}: {}", requestId, requestClass.getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }
} 