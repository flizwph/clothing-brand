package com.brand.backend.presentation.rest.controller.auth.v1;

import com.brand.backend.presentation.dto.request.UserLoginRequest;
import com.brand.backend.presentation.dto.request.UserRegistrationRequest;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.infrastructure.security.jwt.JwtUtil;
import com.brand.backend.application.auth.service.facade.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * DEPRECATED: This controller has been replaced by v2 API in AuthControllerCQRS.
 * This class is kept only for backward compatibility and will be removed in a future version.
 * 
 * Use /api/auth/v2/* endpoints instead.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Deprecated(forRemoval = true)
// Включается только если api.legacy.direct-implementation=true
@ConditionalOnProperty(name = "api.legacy.direct-implementation", havingValue = "true", matchIfMissing = false)
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody @Valid UserRegistrationRequest request) {
        // Original implementation preserved for reference but not used anymore
        // See LegacyAuthControllerProxy for the actual implementation
        return null;
    }

    @PostMapping("/login")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody @Valid UserLoginRequest request) {
        // Original implementation preserved for reference but not used anymore
        // See LegacyAuthControllerProxy for the actual implementation
        return null;
    }

    @PostMapping("/refresh")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Map<String, String>> refreshAccessToken(@RequestBody Map<String, String> request) {
        // Original implementation preserved for reference but not used anymore
        // See LegacyAuthControllerProxy for the actual implementation
        return null;
    }

    @PostMapping("/logout")
    @Deprecated(forRemoval = true)
    public ResponseEntity<?> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        // Original implementation preserved for reference but not used anymore
        // See LegacyAuthControllerProxy for the actual implementation
        return null;
    }
}
