package com.brand.backend.domain.user.event;

import com.brand.backend.domain.user.model.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserEvent extends ApplicationEvent {

    private final User user;
    private final UserEventType eventType;

    public UserEvent(Object source, User user, UserEventType eventType) {
        super(source);
        this.user = user;
        this.eventType = eventType;
    }

    public enum UserEventType {
        REGISTERED,
        UPDATED,
        LINKED_TELEGRAM,
        LINKED_DISCORD,
        VERIFIED
    }
} 