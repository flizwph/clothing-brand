package com.brand.backend.domain.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

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

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 1;
}
