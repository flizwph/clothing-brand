package com.brand.backend.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Фильтр для добавления уникального идентификатора запроса
 * Идентификатор сохраняется в MDC для логирования и добавляется в заголовок ответа
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Получаем ID запроса из заголовка, если он был предоставлен клиентом
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            
            // Если ID не был предоставлен, генерируем новый
            if (requestId == null || requestId.isEmpty()) {
                requestId = generateRequestId();
            }
            
            // Сохраняем ID в MDC для логирования
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            
            // Добавляем ID в заголовок ответа
            response.setHeader(REQUEST_ID_HEADER, requestId);
            
            // Продолжаем выполнение цепочки фильтров
            filterChain.doFilter(request, response);
        } finally {
            // Очищаем MDC, чтобы избежать утечек памяти
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
    
    /**
     * Генерирует уникальный идентификатор запроса
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
} 