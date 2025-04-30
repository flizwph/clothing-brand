package com.brand.backend.application.auth.bus.impl;

import com.brand.backend.application.auth.bus.QueryBus;
import com.brand.backend.application.auth.mediator.Mediator;
import com.brand.backend.application.auth.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Простая реализация шины запросов, использующая медиатор
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleQueryBus implements QueryBus {

    private final Mediator mediator;

    /**
     * Отправляет запрос на обработку через медиатор
     */
    @Override
    public <R, Q extends Query<R>> R dispatch(Q query) {
        log.debug("Шина запросов: отправка запроса {}", query.getClass().getSimpleName());
        return mediator.send(query);
    }
} 