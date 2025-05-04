package com.brand.backend.common.exeption;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.brand.backend.application.auth.core.exception.UserNotVerifiedException;
import org.slf4j.MDC;
import java.time.ZonedDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    // Структура ответа с ошибкой
    private static class ApiError {
        private final String timestamp;
        private final int status;
        private final String error;
        private final String message;
        private final String path;

        public ApiError(HttpStatus status, String message, String path) {
            this.timestamp = LocalDateTime.now().toString();
            this.status = status.value();
            this.error = status.getReasonPhrase();
            this.message = message;
            this.path = path;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }
    }

    // Обработка ResourceNotFoundException (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND, 
                ex.getMessage(), 
                request.getDescription(false));
        
        log.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // Обработка валидационных ошибок (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        log.warn("Validation error: {}", errors);
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // Обработка ConstraintViolationException (400)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST, 
                "Ошибка валидации: " + ex.getMessage(), 
                request.getDescription(false));
        
        log.warn("Constraint violation: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Обработка EntityNotFoundException (404)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(
            EntityNotFoundException ex, WebRequest request) {
        
        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND, 
                ex.getMessage(), 
                request.getDescription(false));
        
        log.warn("Entity not found: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // Обработка AccessDeniedException (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        
        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN, 
                "Доступ запрещен", 
                request.getDescription(false));
        
        log.warn("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // Обработка BadCredentialsException (401)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        
        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED, 
                "Неверные учетные данные", 
                request.getDescription(false));
        
        log.warn("Bad credentials: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // Обработка UserNotFoundException (404)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(
            UserNotFoundException ex, WebRequest request) {
        
        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND, 
                ex.getMessage(), 
                request.getDescription(false));
        
        log.warn("User not found: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // Обработка нарушений целостности данных (уникальность, внешние ключи и т.д.)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        
        String message = "Ошибка целостности данных";
        String path = request.getDescription(false);
        HttpStatus status = HttpStatus.CONFLICT;
        
        // Анализ сообщения об ошибке для более точного ответа
        String exMessage = ex.getMessage();
        if (exMessage != null) {
            if (exMessage.contains("refresh_tokens_user_id_key")) {
                message = "Уже существует активная сессия. Повторите попытку входа.";
                status = HttpStatus.TOO_MANY_REQUESTS; // 429
                log.warn("Конфликт refresh token: {}", path);
            } else if (exMessage.contains("users_username_key")) {
                message = "Пользователь с таким именем уже существует";
                log.warn("Попытка создания дубликата пользователя: {}", path);
            } else if (exMessage.contains("unique constraint") || exMessage.contains("уникальности")) {
                message = "Указанные данные уже используются в системе";
                log.warn("Нарушение ограничения уникальности: {}", path);
            } else {
                log.error("Ошибка целостности данных: {} - {}", path, exMessage);
            }
        }
        
        ApiError error = new ApiError(status, message, path);
        return new ResponseEntity<>(error, status);
    }

    // Обработка любых других исключений (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllUncaughtException(
            Exception ex, WebRequest request) {
        
        ApiError error = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Произошла внутренняя ошибка сервера", 
                request.getDescription(false));
        
        log.error("Uncaught exception", ex);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UserNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleUserNotVerifiedException(UserNotVerifiedException ex, WebRequest request) {
        log.warn("User not verified exception: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .timestamp(ZonedDateTime.now())
                .requestId(MDC.get(REQUEST_ID_HEADER))
                .build();
        
        errorResponse.addAdditionalInfo("error", "User not verified");
        errorResponse.addAdditionalInfo("verificationCode", ex.getVerificationCode());
        errorResponse.addAdditionalInfo("status", "not_verified");
        errorResponse.addAdditionalInfo("message", "Your account is not verified. Please verify your account with Telegram bot using the provided code.");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
}
