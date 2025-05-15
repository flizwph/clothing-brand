package com.brand.backend.presentation.dto.response;

import com.brand.backend.domain.payment.model.Transaction;
import com.brand.backend.domain.payment.model.TransactionStatus;
import com.brand.backend.domain.payment.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO для отображения информации о транзакции
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    
    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String transactionCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long orderId;
    private String statusMessage;
    private String adminComment;
    private String formattedCreatedAt;
    private String formattedUpdatedAt;
    private String typeMessage;
    
    /**
     * Преобразование из сущности в DTO
     */
    public static TransactionDto fromEntity(Transaction transaction) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        
        return TransactionDto.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .transactionCode(transaction.getTransactionCode())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .orderId(transaction.getOrderId())
                .adminComment(transaction.getAdminComment())
                .statusMessage(getStatusMessage(transaction.getStatus()))
                .formattedCreatedAt(transaction.getCreatedAt().format(formatter))
                .formattedUpdatedAt(transaction.getUpdatedAt() != null ? 
                        transaction.getUpdatedAt().format(formatter) : null)
                .typeMessage(getTypeMessage(transaction.getType()))
                .build();
    }
    
    /**
     * Получить понятное для пользователя описание статуса
     */
    private static String getStatusMessage(TransactionStatus status) {
        return switch (status) {
            case PENDING -> "Ожидает подтверждения";
            case COMPLETED -> "Выполнено";
            case REJECTED -> "Отклонено";
            case CANCELLED -> "Отменено пользователем";
            default -> status.toString();
        };
    }
    
    /**
     * Получить понятное для пользователя описание типа транзакции
     */
    private static String getTypeMessage(TransactionType type) {
        return switch (type) {
            case DEPOSIT -> "Пополнение баланса";
            case WITHDRAWAL -> "Вывод средств";
            case ORDER_PAYMENT -> "Оплата заказа";
            default -> type.toString();
        };
    }
} 