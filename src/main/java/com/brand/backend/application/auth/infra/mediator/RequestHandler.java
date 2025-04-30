package com.brand.backend.application.auth.infra.mediator;

/**
 * Интерфейс обработчика запросов медиатора
 * @param <T> тип результата
 * @param <R> тип запроса
 */
public interface RequestHandler<T, R extends Request<T>> {
    
    /**
     * Обрабатывает запрос и возвращает результат
     * 
     * @param request запрос для обработки
     * @return результат обработки запроса
     */
    T handle(R request);
} 