package com.brand.backend.application.auth.infra.mediator;

/**
 * Интерфейс медиатора для обработки команд и запросов
 * Обеспечивает единый способ обмена сообщениями между компонентами
 */
public interface Mediator {
    
    /**
     * Отправляет запрос и возвращает результат
     * 
     * @param request запрос для обработки
     * @param <T> тип результата
     * @param <R> тип запроса
     * @return результат обработки запроса
     */
    <T, R extends Request<T>> T send(R request);
} 