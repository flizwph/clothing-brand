package com.brand.backend.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Компонент для инициализации необходимых данных при запуске приложения
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String hibernateDdlAuto;
    
    @PostConstruct
    public void init() {
        // Проверяем, существует ли таблица security_audit_events
        try {
            log.info("Проверка существования таблицы security_audit_events...");
            
            boolean tableExists = doesTableExist("security_audit_events");
            if (!tableExists) {
                log.warn("Таблица security_audit_events не найдена! Создаю таблицу...");
                createSecurityAuditTable();
                log.info("Таблица security_audit_events успешно создана");
            } else {
                log.info("Таблица security_audit_events существует");
            }
        } catch (Exception e) {
            log.error("Ошибка при инициализации таблицы security_audit_events: {}", e.getMessage(), e);
        }
    }
    
    private boolean doesTableExist(String tableName) {
        try {
            Integer result = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class, tableName.toLowerCase());
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Ошибка при проверке существования таблицы {}: {}", tableName, e.getMessage());
            return false;
        }
    }
    
    private void createSecurityAuditTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS security_audit_events (" +
                    "id SERIAL PRIMARY KEY, " +
                    "event_type VARCHAR(50) NOT NULL, " +
                    "username VARCHAR(255), " +
                    "timestamp TIMESTAMP NOT NULL, " +
                    "ip_address VARCHAR(45), " +
                    "user_agent TEXT, " +
                    "request_id VARCHAR(50), " +
                    "details TEXT, " +
                    "severity VARCHAR(10) NOT NULL" +
                    ")");
            
            // Создаем индексы
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_username ON security_audit_events(username)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON security_audit_events(timestamp)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_event_type ON security_audit_events(event_type)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_severity ON security_audit_events(severity)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_ip ON security_audit_events(ip_address)");
        } catch (Exception e) {
            log.error("Ошибка при создании таблицы security_audit_events: {}", e.getMessage(), e);
            throw e;
        }
    }
} 