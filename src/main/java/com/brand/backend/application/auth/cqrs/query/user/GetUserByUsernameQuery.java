package com.brand.backend.application.auth.cqrs.query.user;

import com.brand.backend.application.auth.cqrs.query.base.Query;
import com.brand.backend.domain.user.model.User;
import lombok.Builder;
import lombok.Data;

/**
 * Запрос для получения пользователя по имени пользователя
 */
@Data
@Builder
public class GetUserByUsernameQuery implements Query<User> {
    private String username;
} 