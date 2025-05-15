package com.brand.backend.common.annotation.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Аннотация для компонентов, которые должны использоваться только в dev-окружении
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Profile("dev")
public @interface DevComponent {
    String value() default "";
} 