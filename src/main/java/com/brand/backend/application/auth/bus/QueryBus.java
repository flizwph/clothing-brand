package com.brand.backend.application.auth.bus;

import com.brand.backend.application.auth.query.Query;

/**
 * Шина запросов для диспетчеризации запросов соответствующим обработчикам
 */
public interface QueryBus {
    
    /**
     * Отправляет запрос соответствующему обработчику и возвращает результат
     * 
     * @param query запрос для обработки
     * @param <R> тип результата
     * @param <Q> тип запроса
     * @return результат обработки запроса
     */
    <R, Q extends Query<R>> R dispatch(Q query);
} 