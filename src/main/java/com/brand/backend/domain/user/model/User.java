package com.brand.backend.domain.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Setter
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false)
    private String role = "customer";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "last_used_verification_code")
    private String lastUsedVerificationCode;

    @Column(name = "telegram_id", unique = true)
    private Long telegramId;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "discord_id", unique = true)
    private Long discordId;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "telegram_username", unique = true)
    private String telegramUsername;

    @Column(name = "discord_username", unique = true)
    private String discordUsername;

    @Column(name = "vk_username", unique = true)
    private String vkUsername;

    @Column(name = "is_linked_discord", nullable = false)
    private boolean isLinkedDiscord = false;

    @Column(name = "is_linked_vkontakte", nullable = false)
    private boolean isLinkedVkontakte = false;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Column(name = "token_version")
    private Integer tokenVersion = 1;

    // Аватарки социальных сетей
    @Column(name = "telegram_avatar_url")
    private String telegramAvatarUrl;

    @Column(name = "discord_avatar_url")
    private String discordAvatarUrl;

    @Column(name = "vk_avatar_url")
    private String vkAvatarUrl;

    // VK интеграция
    @Column(name = "vk_id", unique = true)
    private Long vkId;

    // Балансы по валютам
    @Column(name = "balance_rub", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceRub = BigDecimal.ZERO;

    @Column(name = "balance_usd", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceUsd = BigDecimal.ZERO;

    @Column(name = "livium_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal liviumBalance = BigDecimal.ZERO;

    // Статусы подключения для UI
    @Column(name = "is_telegram_linked", nullable = false)
    private boolean isTelegramLinked = false;

    @Column(name = "is_discord_linked", nullable = false) 
    private boolean isDiscordLinked = false;

    @Column(name = "is_vk_linked", nullable = false)
    private boolean isVkLinked = false;

    public Integer getTokenVersion() {
        return tokenVersion != null ? tokenVersion : 1;
    }
}
