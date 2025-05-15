package com.brand.backend.common.service;

import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для работы с информацией о клиенте (IP, User-Agent и т.д.)
 */
@Slf4j
@Service
public class ClientInfoService {
    
    /**
     * Получает IP-адрес клиента из запроса
     */
    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        
        String clientIp = request.getRemoteAddr();
        return clientIp != null ? clientIp : "unknown";
    }
    
    /**
     * Получает информацию о User-Agent клиента
     */
    public Map<String, String> parseUserAgent(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        
        if (request == null) {
            result.put("browser", "unknown");
            result.put("os", "unknown");
            result.put("device", "unknown");
            return result;
        }
        
        String userAgentString = request.getHeader("User-Agent");
        if (userAgentString == null || userAgentString.isEmpty()) {
            result.put("browser", "unknown");
            result.put("os", "unknown");
            result.put("device", "unknown");
            return result;
        }
        
        try {
            UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
            
            result.put("browser", userAgent.getBrowser().getName());
            result.put("browserVersion", userAgent.getBrowserVersion() != null ? 
                    userAgent.getBrowserVersion().toString() : "unknown");
            result.put("os", userAgent.getOperatingSystem().getName());
            result.put("deviceType", userAgent.getOperatingSystem().getDeviceType().getName());
            result.put("userAgentString", userAgentString);
        } catch (Exception e) {
            log.warn("Ошибка при парсинге User-Agent: {}", e.getMessage());
            result.put("browser", "unknown");
            result.put("os", "unknown");
            result.put("device", "unknown");
            result.put("userAgentString", userAgentString);
        }
        
        return result;
    }
    
    /**
     * Получает дополнительную информацию о запросе
     */
    public Map<String, String> getRequestInfo(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        
        if (request == null) {
            return result;
        }
        
        result.put("method", request.getMethod());
        result.put("protocol", request.getProtocol());
        result.put("secure", String.valueOf(request.isSecure()));
        result.put("referer", request.getHeader("Referer") != null ? request.getHeader("Referer") : "none");
        result.put("acceptLanguage", request.getHeader("Accept-Language") != null ? 
                request.getHeader("Accept-Language") : "none");
        
        return result;
    }
} 