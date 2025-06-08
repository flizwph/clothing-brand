package com.brand.backend.application.user.service;

import com.brand.backend.presentation.dto.request.UserDTO;
import com.brand.backend.presentation.dto.response.SubscriptionInfoDTO;
import com.brand.backend.presentation.dto.response.UserStatsDTO;
import com.brand.backend.common.exception.UserNotFoundException;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.domain.order.repository.OrderRepository;
import com.brand.backend.domain.nft.repository.NFTRepository;
import com.brand.backend.domain.subscription.repository.SubscriptionRepository;
import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.order.model.OrderStatus;
import com.brand.backend.domain.subscription.model.Subscription;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.brand.backend.domain.user.event.UserEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final NFTRepository nftRepository;
    private final SubscriptionRepository subscriptionRepository;
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
        
        // Новые поля профиля
        dto.setLiviumBalance(user.getLiviumBalance());
        dto.setDiscordAvatarUrl(user.getDiscordAvatarUrl());
        dto.setTelegramAvatarUrl(user.getTelegramAvatarUrl());
        dto.setVkAvatarUrl(user.getVkAvatarUrl());
        dto.setVkId(user.getVkId());
        
        // Статусы привязки интеграций
        dto.setDiscordLinked(user.isDiscordLinked());
        dto.setTelegramLinked(user.isTelegramLinked());
        dto.setVkLinked(user.isVkLinked());

        return dto;
    }

    // ✅ Проверка верификации Telegram
    public boolean isTelegramVerified() {
        String username = getAuthenticatedUsername();
        return getUserByUsername(username).isVerified();
    }

    // ✅ Обновление профиля пользователя с переданным объектом User
    @Transactional
    public void updateUserProfile(User user, String newUsername, String newEmail, String newPhoneNumber) {
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
                user.getUsername(), newUsername, newEmail, newPhoneNumber);
    }

    // ✅ Смена пароля с переданным объектом User
    @Transactional
    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid old password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("✅ [PASSWORD CHANGED] Пароль обновлен для {}", user.getUsername());
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
        user.setTelegramLinked(true);
        user.setUpdatedAt(LocalDateTime.now());
        user.setVerified(true);
        
        User savedUser = userRepository.save(user);
        
        // Публикуем событие привязки Telegram
        eventPublisher.publishEvent(new UserEvent(this, savedUser, UserEvent.UserEventType.LINKED_TELEGRAM));
        
        log.info("Telegram аккаунт привязан к пользователю: {}", username);
    }

    @Transactional
    public void linkTelegramAccountWithAvatar(String username, Long telegramId, String telegramUsername, String avatarUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        user.setTelegramId(telegramId);
        user.setTelegramUsername(telegramUsername);
        user.setTelegramAvatarUrl(avatarUrl);
        user.setTelegramLinked(true);
        user.setUpdatedAt(LocalDateTime.now());
        user.setVerified(true);
        
        User savedUser = userRepository.save(user);
        
        // Публикуем событие привязки Telegram
        eventPublisher.publishEvent(new UserEvent(this, savedUser, UserEvent.UserEventType.LINKED_TELEGRAM));
        
        log.info("Telegram аккаунт с аватаркой привязан к пользователю: {}", username);
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
        user.setDiscordLinked(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        // Публикуем событие привязки Discord
        eventPublisher.publishEvent(new UserEvent(this, user, UserEvent.UserEventType.LINKED_DISCORD));
        
        log.info("Discord аккаунт привязан к пользователю: {}", username);
        return true;
    }

    /**
     * Привязывает Discord аккаунт к пользователю с аватаркой
     *
     * @param username имя пользователя
     * @param discordId идентификатор пользователя в Discord
     * @param discordUsername имя пользователя в Discord
     * @param avatarUrl URL аватарки пользователя
     * @return true, если привязка успешна
     */
    @Transactional
    public boolean linkDiscordAccountWithAvatar(String username, Long discordId, String discordUsername, String avatarUrl) {
        User user = getUserByUsername(username);
        
        user.setDiscordId(discordId);
        user.setDiscordUsername(discordUsername);
        user.setDiscordAvatarUrl(avatarUrl);
        user.setLinkedDiscord(true);
        user.setDiscordLinked(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        // Публикуем событие привязки Discord
        eventPublisher.publishEvent(new UserEvent(this, user, UserEvent.UserEventType.LINKED_DISCORD));
        
        log.info("Discord аккаунт с аватаркой привязан к пользователю: {}", username);
        return true;
    }

    /**
     * Привязывает VK аккаунт к пользователю
     *
     * @param username имя пользователя
     * @param vkId идентификатор пользователя в VK
     * @param vkUsername имя пользователя в VK
     * @param avatarUrl URL аватарки пользователя
     * @return true, если привязка успешна
     */
    @Transactional
    public boolean linkVkAccount(String username, Long vkId, String vkUsername, String avatarUrl) {
        User user = getUserByUsername(username);
        
        user.setVkId(vkId);
        user.setVkUsername(vkUsername);
        user.setVkAvatarUrl(avatarUrl);
        user.setLinkedVkontakte(true);
        user.setVkLinked(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        // Публикуем событие привязки VK
        eventPublisher.publishEvent(new UserEvent(this, user, UserEvent.UserEventType.LINKED_VK));
        
        log.info("VK аккаунт с аватаркой привязан к пользователю: {}", username);
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

    /**
     * Преобразует доменный объект User в DTO
     */
    public UserDTO convertToDTO(User user) {
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
        // Новые поля
        dto.setLiviumBalance(user.getLiviumBalance());
        dto.setDiscordAvatarUrl(user.getDiscordAvatarUrl());
        dto.setTelegramAvatarUrl(user.getTelegramAvatarUrl());
        dto.setVkAvatarUrl(user.getVkAvatarUrl());
        dto.setVkId(user.getVkId());
        // Статусы привязки интеграций
        dto.setDiscordLinked(user.isDiscordLinked());
        dto.setTelegramLinked(user.isTelegramLinked());
        dto.setVkLinked(user.isVkLinked());
        return dto;
    }

    /**
     * Отвязывает Discord аккаунт от пользователя
     *
     * @param username имя пользователя
     * @return true, если отвязка успешна
     */
    @Transactional
    public boolean unlinkDiscordAccount(String username) {
        User user = getUserByUsername(username);
        
        if (!user.isLinkedDiscord()) {
            log.warn("Попытка отвязать Discord аккаунт у пользователя без привязки: {}", username);
            return false;
        }
        
        // Сохраняем старые данные для лога
        String oldDiscordUsername = user.getDiscordUsername();
        Long oldDiscordId = user.getDiscordId();
        
        // Отвязываем аккаунт
        user.setDiscordId(null);
        user.setDiscordUsername(null);
        user.setLinkedDiscord(false);
        user.setDiscordLinked(false);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        // Публикуем событие отвязки Discord
        eventPublisher.publishEvent(new UserEvent(this, user, UserEvent.UserEventType.UNLINKED_DISCORD));
        
        log.info("Discord аккаунт отвязан от пользователя: {}, был discordId={}, discordUsername={}", 
                username, oldDiscordId, oldDiscordUsername);
        return true;
    }

    /**
     * Получение статистики профиля пользователя
     */
    public UserStatsDTO getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Подсчет активных заказов (все кроме COMPLETED)
        int activeOrders = (int) orderRepository.findByUser(user).stream()
                .filter(order -> order.getStatus() != OrderStatus.COMPLETED)
                .count();

        // Подсчет NFT коллекции
        int nftCollection = nftRepository.findByUser(user).size();

        // Подсчет общей истории заказов
        int orderHistory = orderRepository.findByUser(user).size();

        return new UserStatsDTO(activeOrders, nftCollection, orderHistory);
    }

    /**
     * Получение информации о подписке пользователя
     */
    public SubscriptionInfoDTO getUserSubscriptionInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndIsActiveTrue(user);
        
        if (activeSubscriptions.isEmpty()) {
            // Возвращаем информацию о том, что подписки нет
            SubscriptionInfoDTO dto = new SubscriptionInfoDTO();
            dto.setLevel("NONE");
            dto.setActive(false);
            dto.setStartDate(null);
            dto.setEndDate(null);
            dto.setPrice(BigDecimal.ZERO);
            dto.setName("Нет активной подписки");
            dto.setAutoRenewal(false);
            dto.setDaysRemaining(0);
            dto.setNextBillingDate(null);
            dto.setLimits(null);
            dto.setUsage(null);
            return dto;
        }

        // Берем первую активную подписку (предполагаем, что у пользователя одна активная подписка)
        Subscription subscription = activeSubscriptions.get(0);

        SubscriptionInfoDTO dto = new SubscriptionInfoDTO();
        dto.setLevel(subscription.getSubscriptionLevel().name());
        dto.setActive(subscription.isActive());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        dto.setPrice(getSubscriptionPrice(subscription.getSubscriptionLevel().name()));
        dto.setName(getSubscriptionName(subscription.getSubscriptionLevel().name()));
        dto.setAutoRenewal(subscription.isAutoRenewal());
        dto.setDaysRemaining(calculateDaysRemaining(subscription.getEndDate()));
        dto.setNextBillingDate(subscription.isAutoRenewal() ? subscription.getEndDate() : null);
        dto.setLimits(null); // TODO: добавить лимиты подписки
        dto.setUsage(null);  // TODO: добавить использование подписки
        return dto;
    }

    /**
     * Вспомогательный метод для получения цены подписки
     */
    private BigDecimal getSubscriptionPrice(String level) {
        return switch (level) {
            case "BASIC" -> new BigDecimal("299");
            case "STANDARD" -> new BigDecimal("599");
            case "PREMIUM" -> new BigDecimal("999");
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Вспомогательный метод для получения названия подписки
     */
    private String getSubscriptionName(String level) {
        return switch (level) {
            case "BASIC" -> "Базовая подписка";
            case "STANDARD" -> "Стандартная подписка";
            case "PREMIUM" -> "Премиум подписка";
            default -> "Неизвестная подписка";
        };
    }

    /**
     * Вспомогательный метод для подсчета оставшихся дней подписки
     */
    private int calculateDaysRemaining(LocalDateTime endDate) {
        if (endDate == null) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), endDate);
    }

    /**
     * Привязка Discord аккаунта с аватаркой
     */
    @Transactional
    public void linkDiscordAccount(Long userId, Long discordId, String discordUsername, String discordAvatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));
        
        user.setDiscordId(discordId);
        user.setDiscordUsername(discordUsername);
        user.setDiscordAvatarUrl(discordAvatarUrl);
        user.setDiscordLinked(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        // Публикуем событие привязки Discord
        eventPublisher.publishEvent(new UserEvent(this, user, UserEvent.UserEventType.LINKED_DISCORD));
        
        log.info("Discord аккаунт {} привязан к пользователю {}", discordUsername, user.getUsername());
    }

    /**
     * Привязка Telegram аккаунта с аватаркой
     */
    @Transactional
    public void linkTelegramAccount(Long userId, Long telegramId, String telegramUsername, String telegramAvatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));
        
        user.setTelegramId(telegramId);
        user.setTelegramUsername(telegramUsername);
        user.setTelegramAvatarUrl(telegramAvatarUrl);
        user.setTelegramLinked(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        log.info("Telegram аккаунт {} привязан к пользователю {}", telegramUsername, user.getUsername());
    }

    /**
     * Привязка VK аккаунта с аватаркой
     */
    @Transactional
    public void linkVkAccount(Long userId, String vkId, String vkUsername, String vkAvatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));
        
        user.setVkId(Long.valueOf(vkId));
        user.setVkUsername(vkUsername);
        user.setVkAvatarUrl(vkAvatarUrl);
        user.setVkLinked(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        // Публикуем событие привязки VK
        eventPublisher.publishEvent(new UserEvent(this, user, UserEvent.UserEventType.LINKED_VK));
        
        log.info("VK аккаунт {} привязан к пользователю {}", vkUsername, user.getUsername());
    }

    /**
     * Поиск пользователя по имени (возвращает Optional для совместимости)
     */
    public java.util.Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
