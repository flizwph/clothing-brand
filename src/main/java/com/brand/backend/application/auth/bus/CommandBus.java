package com.brand.backend.application.auth.bus;

import com.brand.backend.application.auth.cqrs.command.base.Command;

/**
 * Шина команд для диспетчеризации команд соответствующим обработчикам
 */
public interface CommandBus {
    
    /**
     * Отправляет команду соответствующему обработчику и возвращает результат
     * 
     * @param command команда для обработки
     * @param <R> тип результата
     * @param <C> тип команды
     * @return результат обработки команды
     */
    <R, C extends Command<R>> R dispatch(C command);
} 