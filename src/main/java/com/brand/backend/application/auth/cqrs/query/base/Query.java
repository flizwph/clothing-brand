package com.brand.backend.application.auth.cqrs.query.base;

import com.brand.backend.application.auth.infra.mediator.Request;

/**
 * Базовый интерфейс для всех запросов в паттерне CQRS
 * Запросы используются для операций чтения данных без изменения состояния
 * @param <R> тип возвращаемого результата
 */
public interface Query<R> extends Request<R> {
    // Пустой маркерный интерфейс, расширяющий интерфейс Request из медиатора
} 