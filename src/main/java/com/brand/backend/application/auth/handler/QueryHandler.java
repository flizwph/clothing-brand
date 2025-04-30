package com.brand.backend.application.auth.handler;

import com.brand.backend.application.auth.query.Query;
import com.brand.backend.application.auth.mediator.RequestHandler;

/**
 * Базовый интерфейс для всех обработчиков запросов в паттерне CQRS
 * @param <Q> тип запроса
 * @param <R> тип возвращаемого результата
 */
public interface QueryHandler<Q extends Query<R>, R> extends RequestHandler<R, Q> {
    // Расширяет RequestHandler из медиатора
} 