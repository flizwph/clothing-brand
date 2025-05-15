package com.brand.backend.infrastructure.security.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность для записи событий аудита безопасности
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "security_audit_events", indexes = {
    @Index(name = "idx_audit_username", columnList = "username"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_severity", columnList = "severity"),
    @Index(name = "idx_audit_ip", columnList = "ip_address")
})
public class SecurityAuditEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "request_id")
    private String requestId;
    
    @Column(name = "details", length = 1000)
    private String details;
    
    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private SecurityEventSeverity severity;
} 