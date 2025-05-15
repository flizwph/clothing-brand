package com.brand.backend.infrastructure.security.jwt;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
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
import java.util.Optional;

/**
 * Фильтр для аутентификации по JWT-токену
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        log.debug("TokenAuthenticationFilter обрабатывает запрос к: {}", path);
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (jwt != null) {
                log.debug("Найден JWT токен в запросе");
                String username = jwtUtil.extractUsername(jwt);
                log.debug("Извлечено имя пользователя из токена: {}", username);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    log.debug("Пытаемся загрузить пользователя: {}", username);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    log.debug("Пользователь успешно загружен: {}", username);
                    
                    // Проверяем версию токена
                    boolean isTokenValid = jwtUtil.isTokenValid(jwt, userDetails);
                    boolean isVersionValid = isTokenVersionValid(jwt, username);
                    log.debug("Токен валиден: {}, версия валидна: {}", isTokenValid, isVersionValid);
                    
                    if (isTokenValid && isVersionValid) {
                        // Получаем доменного пользователя
                        User domainUser = userRepository.findByUsername(username)
                                .orElse(null);
                        
                        if (domainUser == null) {
                            log.error("Пользователь не найден в базе данных: {}", username);
                            filterChain.doFilter(request, response);
                            return;
                        }
                        
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                domainUser,  // Устанавливаем доменного пользователя как принципал
                                null, 
                                userDetails.getAuthorities());
                        
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Аутентификация успешно установлена в контексте безопасности для пользователя: {}", username);
                    } else {
                        log.warn("Токен не прошел валидацию для пользователя: {}", username);
                    }
                }
            } else {
                log.debug("JWT токен не найден в запросе");
            }
        } catch (Exception e) {
            log.error("Не удалось установить аутентификацию пользователя: {}", e.getMessage(), e);
        }
        
        log.debug("Продолжаем обработку запроса к: {}", path);
        filterChain.doFilter(request, response);
    }
    
    /**
     * Проверяет, соответствует ли версия токена текущей версии пользователя
     */
    private boolean isTokenVersionValid(String jwt, String username) {
        try {
            Integer tokenVersion = jwtUtil.extractTokenVersion(jwt);
            if (tokenVersion == null) {
                // Если версия токена не указана (старый токен), считаем его равным 1
                log.debug("Токен не содержит версии для пользователя: {}, устанавливаем версию 1", username);
                tokenVersion = 1;
            }
            
            log.debug("Поиск пользователя в базе: {}", username);
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                log.warn("Пользователь не найден в базе данных: {}", username);
                return false;
            }
            
            User user = userOpt.get();
            log.debug("Пользователь найден: {}, id: {}", username, user.getId());
            if (user.getTokenVersion() == null) {
                // Если у пользователя не указана версия, обновляем до 1
                log.debug("У пользователя {} отсутствует версия токена, устанавливаем 1", username);
                user.setTokenVersion(1);
                userRepository.save(user);
                return tokenVersion.equals(1);
            }
            
            boolean isValid = tokenVersion.equals(user.getTokenVersion());
            if (!isValid) {
                log.warn("Версия токена ({}) не соответствует версии пользователя ({}) для {}", 
                        tokenVersion, user.getTokenVersion(), username);
            } else {
                log.debug("Версия токена валидна для пользователя {}: {}", username, tokenVersion);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Ошибка при проверке версии токена для {}: {}", username, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Извлекает JWT-токен из заголовка Authorization
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
} 