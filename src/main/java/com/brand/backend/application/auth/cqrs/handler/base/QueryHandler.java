package com.brand.backend.application.auth.cqrs.handler.base;

import com.brand.backend.application.auth.cqrs.query.base.Query;
import com.brand.backend.application.auth.infra.mediator.RequestHandler;

/**
 * Базовый интерфейс для всех обработчиков запросов в паттерне CQRS
 * @param <Q> тип запроса
 * @param <R> тип возвращаемого результата
 */
public interface QueryHandler<Q extends Query<R>, R> extends RequestHandler<R, Q> {
    // Расширяет RequestHandler из медиатора
} 