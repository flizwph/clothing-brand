package com.brand.backend.application.auth.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение для ошибок при восстановлении пароля
 */
public class PasswordResetException extends AuthException {

    private static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;
    private static final String ERROR_CODE = "AUTH_PASSWORD_RESET_ERROR";
    
    public PasswordResetException(String message) {
        super(message, STATUS, ERROR_CODE);
    }
    
    /**
     * Исключение для случая, когда пользователь не найден
     */
    public static PasswordResetException userNotFound(String username) {
        return new PasswordResetException(String.format("User %s not found", username));
    }
    
    /**
     * Исключение для случая, когда пользователь не верифицирован
     */
    public static PasswordResetException userNotVerified(String username) {
        return new PasswordResetException(String.format("User %s is not verified", username));
    }
    
    /**
     * Исключение для случая, когда у пользователя нет привязки к Telegram
     */
    public static PasswordResetException noTelegramAccount(String username) {
        return new PasswordResetException(String.format("User %s has no Telegram account linked", username));
    }
    
    /**
     * Исключение для случая, когда уже есть активный запрос на восстановление пароля
     */
    public static PasswordResetException activeRequestExists(String username) {
        return new PasswordResetException(String.format(
                "An active password reset request already exists for user %s. Please wait before requesting again",
                username));
    }
    
    /**
     * Исключение для случая, когда код восстановления недействителен
     */
    public static PasswordResetException invalidResetCode() {
        return new PasswordResetException("Invalid or expired reset code");
    }
} 