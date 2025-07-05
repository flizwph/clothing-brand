package com.brand.backend.common.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * Фильтр для добавления уникального идентификатора запроса
 * Идентификатор сохраняется в MDC для логирования и добавляется в заголовок ответа
 */
@Component
public class RequestIdFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        
        try {
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
} 