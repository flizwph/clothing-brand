package com.brand.backend.application.auth.bus.impl;

import com.brand.backend.application.auth.bus.CommandBus;
import com.brand.backend.application.auth.cqrs.command.base.Command;
import com.brand.backend.application.auth.infra.mediator.Mediator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Простая реализация шины команд, использующая медиатор
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleCommandBus implements CommandBus {

    private final Mediator mediator;

    /**
     * Отправляет команду на обработку через медиатор
     */
    @Override
    public <R, C extends Command<R>> R dispatch(C command) {
        log.debug("Шина команд: отправка команды {}", command.getClass().getSimpleName());
        return mediator.send(command);
    }
} 