package com.brand.backend.infrastructure.config;

import com.brand.backend.application.auth.service.facade.AuthService;
import com.brand.backend.application.order.service.OrderService;
import com.brand.backend.application.product.service.ProductService;
import com.brand.backend.application.nft.service.NFTService;
import com.brand.backend.application.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
public class InteractionConfig {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final AuthService authService;
    private final NFTService nftService;

    @Bean
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }
} 