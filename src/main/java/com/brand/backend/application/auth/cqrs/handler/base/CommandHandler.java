package com.brand.backend.application.auth.cqrs.handler.base;

import com.brand.backend.application.auth.cqrs.command.base.Command;
import com.brand.backend.application.auth.infra.mediator.RequestHandler;

/**
 * Базовый интерфейс для всех обработчиков команд в паттерне CQRS
 * @param <C> тип команды
 * @param <R> тип возвращаемого результата
 */
public interface CommandHandler<C extends Command<R>, R> extends RequestHandler<R, C> {
    // Расширяет RequestHandler из медиатора
} 