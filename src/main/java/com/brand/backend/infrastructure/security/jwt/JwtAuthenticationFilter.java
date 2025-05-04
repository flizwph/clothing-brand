package com.brand.backend.infrastructure.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import org.slf4j.MDC;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;  // Загружаем пользователя по логину

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = MDC.get("requestId");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            MDC.put("requestId", requestId);
        }
        
        String requestURI = request.getRequestURI();
        log.debug("[{}] JwtAuthenticationFilter обрабатывает запрос к URI: {}", requestId, requestURI);
        
        final String authHeader = request.getHeader("Authorization");
        log.debug("Обработка запроса на URI: {}", requestURI);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Отсутствует или неверный формат заголовка Authorization");
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        final String username;

        try {
            username = jwtUtil.extractUsername(token);
            log.debug("Имя пользователя из токена: {}", username);
        } catch (ExpiredJwtException e) {
            log.warn("⚠️ [JWT ERROR] Токен просрочен: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
            return;
        } catch (Exception e) {
            log.error("❌ [JWT ERROR] Ошибка обработки токена: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // ✅ Загружаем пользователя из базы, а не передаем `String`
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                log.debug("[{}] Начало проверки валидности токена для пользователя: {}", requestId, username);
                log.debug("[{}] Пытаемся загрузить пользователя: {}", requestId, username);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.debug("[{}] UserDetails успешно загружен для пользователя: {}", requestId, username);

                if (jwtUtil.isTokenValid(token, userDetails)) {
                    log.debug("[{}] Токен действителен для пользователя: {}", requestId, username);
                    log.debug("[{}] Токен для пользователя {} действителен, создаем аутентификацию", requestId, username);
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("[{}] Аутентификация успешно установлена для пользователя: {}", requestId, username);
                } else {
                    log.warn("[{}] Недействительный токен для пользователя: {}", requestId, username);
                }
            } catch (Exception e) {
                log.error("[{}] Ошибка при загрузке пользователя {}: {}", requestId, username, e.getMessage(), e);
            }
        }

        log.debug("[{}] Продолжение цепочки фильтров для URI: {}", requestId, requestURI);
        filterChain.doFilter(request, response);
    }
}
