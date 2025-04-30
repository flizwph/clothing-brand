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

        final String authHeader = request.getHeader("Authorization");
        log.debug("Обработка запроса на URI: {}", request.getRequestURI());

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
                log.debug("Пытаемся загрузить пользователя: {}", username);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.debug("Пользователь {} успешно загружен", username);

                if (jwtUtil.isTokenValid(token, userDetails)) {
                    log.debug("Токен для пользователя {} действителен, создаем аутентификацию", username);
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Аутентификация успешно установлена в контексте безопасности");
                } else {
                    log.warn("Токен недействителен для пользователя: {}", username);
                }
            } catch (Exception e) {
                log.error("❌ Ошибка при загрузке пользователя {}: {}", username, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
