package com.brand.backend.application.auth.service.facade;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.infrastructure.security.audit.SecurityAuditService;
import com.brand.backend.infrastructure.security.audit.SecurityEventSeverity;
import com.brand.backend.infrastructure.security.audit.SecurityEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Сервис для управления жизненным циклом аккаунтов пользователей
 */
@Slf4j
@Service
public class AccountManagementService {
    
    private final UserRepository userRepository;
    private final SecurityAuditService securityAuditService;
    
    @Value("${security.account.inactive-days-before-disable:90}")
    private int inactiveDaysBeforeDisable;
    
    @Value("${security.account.job-enabled:true}")
    private boolean accountManagementJobEnabled;
    
    public AccountManagementService(UserRepository userRepository,
                                  SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.securityAuditService = securityAuditService;
    }
    
    /**
     * Запланированная задача для блокировки неактивных аккаунтов
     * По умолчанию выполняется каждый день в 2 часа ночи
     */
    @Scheduled(cron = "${security.account.job-cron:0 0 2 * * ?}")
    @Transactional
    public void disableInactiveAccounts() {
        if (!accountManagementJobEnabled) {
            log.debug("Задача блокировки неактивных аккаунтов отключена");
            return;
        }
        
        log.info("Запуск задачи блокировки неактивных аккаунтов");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minus(inactiveDaysBeforeDisable, ChronoUnit.DAYS);
        
        List<User> inactiveUsers = userRepository.findByIsActiveAndLastLoginBefore(true, cutoffDate);
        log.info("Найдено {} неактивных пользователей", inactiveUsers.size());
        
        for (User user : inactiveUsers) {
            user.setActive(false);
            
            // Аудит отключения аккаунта
            securityAuditService.auditEvent(
                    SecurityEventType.ACCOUNT_DISABLED,
                    user.getUsername(),
                    String.format("Аккаунт отключен из-за неактивности в течение %d дней", inactiveDaysBeforeDisable),
                    SecurityEventSeverity.INFO
            );
            
            log.info("Аккаунт {} отключен из-за неактивности", user.getUsername());
        }
        
        if (!inactiveUsers.isEmpty()) {
            userRepository.saveAll(inactiveUsers);
        }
    }
    
    /**
     * Активирует ранее отключенный аккаунт
     */
    @Transactional
    public boolean reactivateAccount(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user == null) {
            log.warn("Попытка активации несуществующего аккаунта: {}", username);
            return false;
        }
        
        if (user.isActive()) {
            log.debug("Аккаунт {} уже активен", username);
            return true;
        }
        
        user.setActive(true);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        // Аудит включения аккаунта
        securityAuditService.auditEvent(
                SecurityEventType.ACCOUNT_UNLOCKED,
                username,
                "Аккаунт активирован вручную",
                SecurityEventSeverity.INFO
        );
        
        log.info("Аккаунт {} успешно активирован", username);
        return true;
    }
    
    /**
     * Вручную отключает аккаунт пользователя
     */
    @Transactional
    public boolean disableAccount(String username, String reason) {
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user == null) {
            log.warn("Попытка отключения несуществующего аккаунта: {}", username);
            return false;
        }
        
        if (!user.isActive()) {
            log.debug("Аккаунт {} уже отключен", username);
            return true;
        }
        
        user.setActive(false);
        userRepository.save(user);
        
        // Аудит отключения аккаунта
        securityAuditService.auditEvent(
                SecurityEventType.ACCOUNT_DISABLED,
                username,
                "Аккаунт отключен вручную. Причина: " + reason,
                SecurityEventSeverity.INFO
        );
        
        log.info("Аккаунт {} успешно отключен", username);
        return true;
    }
    
    /**
     * Проверяет, активен ли аккаунт
     */
    public boolean isAccountActive(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        return user != null && user.isActive();
    }
} 