package com.brand.backend.application.auth.cqrs.handler.user;

import com.brand.backend.application.auth.cqrs.command.user.RegisterUserCommand;
import com.brand.backend.application.auth.core.exception.InvalidPasswordException;
import com.brand.backend.application.auth.core.exception.UsernameExistsException;
import com.brand.backend.application.auth.cqrs.handler.base.CommandHandler;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Обработчик команды регистрации пользователя
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterUserCommandHandler implements CommandHandler<RegisterUserCommand, User> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User handle(RegisterUserCommand command) {
        log.info("Обработка команды регистрации пользователя: {}", command.getUsername());

        if (command.getPassword() == null || command.getPassword().isEmpty()) {
            log.error("Пароль не может быть пустым!");
            throw InvalidPasswordException.empty();
        }
        
        if (command.getPassword().length() < 6) {
            log.error("Пароль слишком короткий для пользователя: {}", command.getUsername());
            throw InvalidPasswordException.tooShort(6);
        }

        if (userRepository.findByUsername(command.getUsername()).isPresent()) {
            log.warn("Пользователь с таким именем уже существует: {}", command.getUsername());
            throw new UsernameExistsException(command.getUsername());
        }

        User user = new User();
        user.setUsername(command.getUsername());
        user.setPasswordHash(passwordEncoder.encode(command.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setTokenVersion(1);

        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerified(false);

        log.info("Пользователь зарегистрирован: {}", user.getUsername());
        return userRepository.save(user);
    }
    
    private String generateVerificationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 32; i++) {
            int index = random.nextInt(chars.length());
            code.append(chars.charAt(index));
        }

        return code.toString();
    }
} 