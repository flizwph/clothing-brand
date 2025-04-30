package com.brand.backend.application.auth.handler;

import com.brand.backend.application.auth.query.GetUserByUsernameQuery;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Обработчик запроса для получения пользователя по имени пользователя
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetUserByUsernameQueryHandler implements QueryHandler<GetUserByUsernameQuery, User> {

    private final UserRepository userRepository;

    @Override
    public User handle(GetUserByUsernameQuery query) {
        log.info("Обработка запроса на получение пользователя по имени: {}", query.getUsername());
        return userRepository.findByUsername(query.getUsername())
                .orElse(null);
    }
} 