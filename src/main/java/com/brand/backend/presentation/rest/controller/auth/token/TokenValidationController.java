package com.brand.backend.presentation.rest.controller.auth.token;

import com.brand.backend.infrastructure.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * DEPRECATED: Контроллер для проверки JWT-токенов.
 * Заменен на /api/auth/v2/validate-token с методом GET.
 * 
 * @deprecated Используйте /api/auth/v2/validate-token вместо этого
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/token")
@RequiredArgsConstructor
@Deprecated(forRemoval = true)
// Включается только если api.legacy.direct-implementation=true
@ConditionalOnProperty(name = "api.legacy.direct-implementation", havingValue = "true", matchIfMissing = false)
public class TokenValidationController {

    private final JwtUtil jwtUtil;
    
    /**
     * Проверить валидность JWT-токена
     * 
     * @deprecated Используйте /api/auth/v2/validate-token с методом GET вместо этого
     */
    @PostMapping("/validate")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        // Original implementation preserved for reference but not used anymore
        // See LegacyTokenValidationProxy for the actual implementation
        return null;
    }
} 