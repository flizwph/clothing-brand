package com.brand.backend.presentation.rest.advice;

import com.brand.backend.domain.payment.exception.TransactionException;
import com.brand.backend.domain.payment.exception.TransactionNotFoundException;
import com.brand.backend.domain.payment.exception.InvalidTransactionStateException;
import com.brand.backend.domain.payment.exception.UnauthorizedTransactionAccessException;
import com.brand.backend.presentation.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Обработчик исключений для транзакций
 */
@RestControllerAdvice
@Slf4j
public class TREH {

    /**
     * Обработка исключений, связанных с транзакциями
     */
    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ErrorResponse> handleTransactionException(TransactionException ex) {
        log.error("Ошибка при работе с транзакцией: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Обработка исключения "Транзакция не найдена"
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFoundException(TransactionNotFoundException ex) {
        log.error("Транзакция не найдена: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Обработка исключения "Неверное состояние транзакции"
     */
    @ExceptionHandler(InvalidTransactionStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransactionStateException(InvalidTransactionStateException ex) {
        log.error("Недопустимое состояние транзакции: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    /**
     * Обработка исключения "Неавторизованный доступ к транзакции"
     */
    @ExceptionHandler(UnauthorizedTransactionAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedTransactionAccessException(UnauthorizedTransactionAccessException ex) {
        log.error("Попытка неавторизованного доступа к транзакции: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Доступ запрещен")
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
} 