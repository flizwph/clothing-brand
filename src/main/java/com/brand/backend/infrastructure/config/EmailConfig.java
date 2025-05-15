package com.brand.backend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.beans.factory.annotation.Value;

import java.util.Properties;

/**
 * Конфигурация для отправки email-сообщений
 */
@Configuration
public class EmailConfig {

    @Value("${spring.mail.host:smtp.yandex.ru}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:#{null}}")
    private String username;

    @Value("${spring.mail.password:#{null}}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private String starttls;

    /**
     * Создает бин JavaMailSender для отправки email-сообщений
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        
        if (username != null && !username.isEmpty()) {
            mailSender.setUsername(username);
        }
        
        if (password != null && !password.isEmpty()) {
            mailSender.setPassword(password);
        }
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.debug", "false");
        
        return mailSender;
    }
} 