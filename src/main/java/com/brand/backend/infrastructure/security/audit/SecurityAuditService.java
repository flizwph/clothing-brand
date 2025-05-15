package com.brand.backend.infrastructure.security.audit;

import com.brand.backend.common.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Сервис для аудита безопасности 
 * Отвечает за регистрацию всех событий безопасности в системе
 */
@Slf4j
@Service
public class SecurityAuditService {
    
    private final SecurityAuditRepository auditRepository;
    
    public SecurityAuditService(SecurityAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }
    
    /**
     * Создает запись аудита безопасности
     */
    @Async
    public void auditEvent(SecurityEventType eventType, String username, String details, SecurityEventSeverity severity) {
        try {
            HttpServletRequest request = getRequest();
            
            String ipAddress = getClientIp(request);
            String userAgent = getUserAgent(request);
            String requestId = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
            
            SecurityAuditEvent event = SecurityAuditEvent.builder()
                    .eventType(eventType.name())
                    .username(username)
                    .timestamp(LocalDateTime.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .requestId(requestId)
                    .details(details)
                    .severity(severity)
                    .build();
            
            auditRepository.save(event);
            
            log.debug("Аудит: {} [{}] - {}", eventType, severity, details);
        } catch (Exception e) {
            log.error("Ошибка при записи события аудита: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Аудит информационного события
     */
    public void auditInfo(SecurityEventType eventType, String username, String details) {
        auditEvent(eventType, username, details, SecurityEventSeverity.INFO);
    }
    
    /**
     * Аудит события-предупреждения
     */
    public void auditWarning(SecurityEventType eventType, String username, String details) {
        auditEvent(eventType, username, details, SecurityEventSeverity.WARNING);
    }
    
    /**
     * Аудит критического события
     */
    public void auditCritical(SecurityEventType eventType, String username, String details) {
        auditEvent(eventType, username, details, SecurityEventSeverity.CRITICAL);
    }
    
    /**
     * Получает текущий HTTP-запрос
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    /**
     * Получает IP-адрес клиента
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
    
    /**
     * Получает User-Agent клиента
     */
    private String getUserAgent(HttpServletRequest request) {
        return request != null ? 
                Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown") : 
                "unknown";
    }
} 