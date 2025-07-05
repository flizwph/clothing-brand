package com.brand.backend.infrastructure.security.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для работы с событиями аудита безопасности
 */
@Repository
public interface SecurityAuditRepository extends JpaRepository<SecurityAuditEvent, Long> {
    
    /**
     * Находит события аудита по имени пользователя
     */
    List<SecurityAuditEvent> findByUsername(String username);
    
    /**
     * Находит события аудита по типу
     */
    List<SecurityAuditEvent> findByEventType(String eventType);
    
    /**
     * Находит события аудита по уровню важности
     */
    List<SecurityAuditEvent> findBySeverity(SecurityEventSeverity severity);
    
    /**
     * Находит события аудита за определенный период времени
     */
    List<SecurityAuditEvent> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Находит события аудита по IP-адресу
     */
    List<SecurityAuditEvent> findByIpAddress(String ipAddress);
    
    /**
     * Находит события по комбинации параметров
     */
    List<SecurityAuditEvent> findByUsernameAndEventTypeAndTimestampBetween(
            String username, String eventType, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Найти события по типу и уровню важности
     */
    Page<SecurityAuditEvent> findByEventTypeAndSeverity(String eventType, SecurityEventSeverity severity, Pageable pageable);
    
    /**
     * Найти последние события с критической важностью
     */
    List<SecurityAuditEvent> findBySeverityOrderByTimestampDesc(SecurityEventSeverity severity, Pageable pageable);
    
    /**
     * Поиск подозрительной активности за последнее время
     */
    @Query("SELECT e FROM SecurityAuditEvent e WHERE e.severity = 'CRITICAL' AND e.timestamp > :since")
    List<SecurityAuditEvent> findSuspiciousActivitiesSince(@Param("since") LocalDateTime since);
    
    /**
     * Найти события по ID запроса (для трассировки)
     */
    List<SecurityAuditEvent> findByRequestId(String requestId);
    
    /**
     * Получить статистику активности пользователей с группировкой по имени пользователя
     * Возвращает массив Object[] где первый элемент - имя пользователя, второй - количество событий
     */
    @Query("SELECT e.username, COUNT(e) FROM SecurityAuditEvent e " +
           "WHERE e.eventType = 'LOGIN_FAILURE' AND e.timestamp > :since " +
           "GROUP BY e.username " +
           "HAVING COUNT(e) >= 3")
    List<Object[]> findUserActivityStatsSince(@Param("since") LocalDateTime since);
    
    /**
     * Получить количество неудачных попыток входа для указанного пользователя за период
     */
    @Query("SELECT COUNT(e) FROM SecurityAuditEvent e " +
           "WHERE e.username = :username AND e.eventType = 'LOGIN_FAILURE' AND e.timestamp > :since")
    long countFailedLoginAttempts(@Param("username") String username, @Param("since") LocalDateTime since);
    
    /**
     * Поиск активности с конкретного IP-адреса за период времени
     */
    @Query("SELECT e FROM SecurityAuditEvent e " +
           "WHERE e.ipAddress = :ipAddress AND e.timestamp > :since " +
           "ORDER BY e.timestamp DESC")
    List<SecurityAuditEvent> findByIpAddressAndSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    /**
     * Удаляет записи аудита по имени пользователя и типам событий
     */
    int deleteByUsernameAndEventTypeIn(String username, java.util.List<String> eventTypes);
} 