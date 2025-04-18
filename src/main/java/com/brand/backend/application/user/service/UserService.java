package com.brand.backend.application.user.service;

import com.brand.backend.presentation.dto.request.UserDTO;
import com.brand.backend.common.exeption.UserNotFoundException;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.brand.backend.domain.user.event.UserEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    // ✅ Получение текущего пользователя
    public UserDTO getCurrentUser() {
        String username = getAuthenticatedUsername();
        User user = getUserByUsername(username);

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        dto.setTelegramId(user.getTelegramId());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setVerificationCode(user.getVerificationCode());
        dto.setVerified(user.isVerified());
        dto.setDiscordId(user.getDiscordId());
        dto.setLastLogin(user.getLastLogin());
        dto.setTelegramUsername(user.getTelegramUsername());
        dto.setDiscordUsername(user.getDiscordUsername());
        dto.setVkUsername(user.getVkUsername());
        dto.setLinkedDiscord(user.isLinkedDiscord());
        dto.setLinkedVkontakte(user.isLinkedVkontakte());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());

        return dto;
    }

    // ✅ Проверка верификации Telegram
    public boolean isTelegramVerified() {
        String username = getAuthenticatedUsername();
        return getUserByUsername(username).isVerified();
    }

    // ✅ Обновление профиля пользователя
    @Transactional
    public void updateUserProfile(String newUsername, String newEmail, String newPhoneNumber) {
        String username = getAuthenticatedUsername();
        User user = getUserByUsername(username);

        if (newUsername != null && !newUsername.isBlank()) {
            user.setUsername(newUsername);
        }
        if (newEmail != null && !newEmail.isBlank()) {
            user.setEmail(newEmail);
        }
        if (newPhoneNumber != null && !newPhoneNumber.isBlank()) {
            user.setPhoneNumber(newPhoneNumber);
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("✅ [USER UPDATED] {} обновил профиль: username={}, email={}, phone={}",
                username, newUsername, newEmail, newPhoneNumber);
    }

    // ✅ Смена пароля
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        String username = getAuthenticatedUsername();
        User user = getUserByUsername(username);

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid old password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("✅ [PASSWORD CHANGED] Пароль обновлен для {}", username);
    }

    // 🔹 Вспомогательные методы
    public String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public void linkTelegramAccount(String username, Long telegramId, String telegramUsername) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        user.setTelegramId(telegramId);
        user.setTelegramUsername(telegramUsername);
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        // Публикуем событие привязки Telegram
        eventPublisher.publishEvent(new UserEvent(this, savedUser, UserEvent.UserEventType.LINKED_TELEGRAM));
        
        log.info("Telegram аккаунт привязан к пользователю: {}", username);
    }
    
    /**
     * Привязывает Discord аккаунт к пользователю
     *
     * @param username имя пользователя
     * @param discordId идентификатор пользователя в Discord
     * @param discordUsername имя пользователя в Discord
     * @return true, если привязка успешна
     */
    @Transactional
    public boolean linkDiscordAccount(String username, Long discordId, String discordUsername) {
        User user = getUserByUsername(username);
        
        user.setDiscordId(discordId);
        user.setDiscordUsername(discordUsername);
        user.setLinkedDiscord(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        // Публикуем событие привязки Discord
        eventPublisher.publishEvent(new UserEvent(this, user, UserEvent.UserEventType.LINKED_DISCORD));
        
        log.info("Discord аккаунт привязан к пользователю: {}", username);
        return true;
    }

    @Transactional
    public User register(String username, String password, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Пользователь с таким именем уже существует");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole("ROLE_USER");
        user.setCreatedAt(LocalDateTime.now());
        
        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        
        User savedUser = userRepository.save(user);
        
        // Публикуем событие регистрации пользователя
        eventPublisher.publishEvent(new UserEvent(this, savedUser, UserEvent.UserEventType.REGISTERED));
        
        return savedUser;
    }
    
    @Transactional
    public void verifyUser(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode)
                .orElseThrow(() -> new RuntimeException("Неверный код верификации"));
        
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        // Публикуем событие верификации пользователя
        eventPublisher.publishEvent(new UserEvent(this, savedUser, UserEvent.UserEventType.VERIFIED));
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    private String generateVerificationCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Transactional
    public String generateAndSaveVerificationCode(String username) {
        User user = getUserByUsername(username);
        String code = generateVerificationCode();
        user.setVerificationCode(code);
        userRepository.save(user);
        return code;
    }
    
    public User getUserByVerificationCode(String code) {
        return userRepository.findByVerificationCode(code).orElse(null);
    }
}
