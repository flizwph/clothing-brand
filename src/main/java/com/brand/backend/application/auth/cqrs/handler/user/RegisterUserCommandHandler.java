package com.brand.backend.application.auth.cqrs.handler.user;

import com.brand.backend.application.auth.cqrs.command.user.RegisterUserCommand;
import com.brand.backend.application.auth.core.exception.InvalidPasswordException;
import com.brand.backend.application.auth.core.exception.UsernameExistsException;
import com.brand.backend.application.auth.exception.UserRegistrationException;
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
        log.info("Обработка команды регистрации пользователя: {} с email: {}", command.getUsername(), command.getEmail());

        // Валидация пароля
        if (command.getPassword() == null || command.getPassword().isEmpty()) {
            log.error("Пароль не может быть пустым!");
            throw UserRegistrationException.invalidPassword();
        }
        
        if (command.getPassword().length() < 6) {
            log.error("Пароль слишком короткий для пользователя: {}", command.getUsername());
            throw UserRegistrationException.invalidPassword();
        }

        // Проверка совпадения паролей
        if (!command.getPassword().equals(command.getConfirmPassword())) {
            log.error("Пароли не совпадают для пользователя: {}", command.getUsername());
            throw UserRegistrationException.passwordsDoNotMatch();
        }

        // Проверка уникальности username
        if (userRepository.findByUsername(command.getUsername()).isPresent()) {
            log.warn("Пользователь с таким именем уже существует: {}", command.getUsername());
            throw UserRegistrationException.usernameAlreadyExists(command.getUsername());
        }

        // Проверка уникальности email
        if (command.getEmail() != null && userRepository.findByEmail(command.getEmail()).isPresent()) {
            log.warn("Пользователь с таким email уже существует: {}", command.getEmail());
            throw UserRegistrationException.emailAlreadyExists(command.getEmail());
        }

        // Создание нового пользователя
        User user = new User();
        user.setUsername(command.getUsername());
        user.setEmail(command.getEmail());
        user.setPasswordHash(passwordEncoder.encode(command.getPassword()));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setTokenVersion(1);

        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerified(false);

        User savedUser = userRepository.save(user);
        log.info("Пользователь зарегистрирован: {} с email: {}, активен: {}", 
                savedUser.getUsername(), savedUser.getEmail(), savedUser.isActive());
        return savedUser;
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