package com.brand.backend.application.auth.command;

import com.brand.backend.application.auth.mediator.Request;

/**
 * Базовый интерфейс для всех команд в паттерне CQRS
 * Команды используются для операций записи/изменения состояния
 * @param <R> тип возвращаемого результата
 */
public interface Command<R> extends Request<R> {
    // Пустой маркерный интерфейс, расширяющий интерфейс Request из медиатора
} 