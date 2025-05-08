package com.brand.backend.domain.user.event;

import com.brand.backend.domain.user.model.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Событие, связанное с пользователем
 */
@Getter
public class UserEvent extends ApplicationEvent {

    private final User user;
    private final UserEventType type;

    /**
     * Создает новое событие пользователя
     * 
     * @param source источник события
     * @param user пользователь
     * @param type тип события
     */
    public UserEvent(Object source, User user, UserEventType type) {
        super(source);
        this.user = user;
        this.type = type;
    }

    /**
     * Типы событий пользователя
     */
    public enum UserEventType {
        REGISTERED,           // Регистрация пользователя
        VERIFIED,             // Верификация пользователя
        LINKED_TELEGRAM,      // Привязка Telegram аккаунта
        LINKED_DISCORD,       // Привязка Discord аккаунта
        UNLINKED_DISCORD,     // Отвязка Discord аккаунта
        LOGGED_IN,            // Вход в систему
        PASSWORD_RESET,       // Сброс пароля
        BALANCE_UPDATED       // Обновление баланса
    }
}