package com.brand.backend.presentation.dto.response;

import com.brand.backend.domain.balance.model.Transaction;
import com.brand.backend.domain.balance.model.TransactionType;
import com.brand.backend.domain.balance.model.TransactionStatus;
import com.brand.backend.domain.balance.model.Currency;
import com.brand.backend.domain.balance.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для отображения информации о транзакции
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Long id;
    private String type;                   // Тип транзакции (DEPOSIT, WITHDRAWAL, etc.)
    private String typeDisplay;            // "Пополнение", "Вывод", etc.
    private String status;                 // Статус (COMPLETED, PENDING, etc.)
    private String statusDisplay;          // "Завершена", "В обработке", etc.
    
    private BigDecimal amount;             // Основная сумма
    private String currency;               // Валюта (RUB, USD, LIV)
    private String amountDisplay;          // "1,234.56 ₽"
    
    // Для конвертаций
    private String fromCurrency;           // Исходная валюта
    private String toCurrency;             // Целевая валюта
    private BigDecimal fromAmount;         // Сумма до конвертации
    private BigDecimal toAmount;           // Сумма после конвертации
    private BigDecimal exchangeRate;       // Курс обмена
    private String conversionDisplay;      // "100.00 USD → 9,000.00 RUB"
    
    private String paymentMethod;          // Способ оплаты
    private String paymentMethodDisplay;   // "Банковская карта"
    private String description;            // Описание
    
    private LocalDateTime createdAt;       // Дата создания
    private LocalDateTime completedAt;     // Дата завершения
    private String timeDisplay;            // "2 часа назад"
    
    /**
     * Статический метод для конвертации Transaction в TransactionDTO
     */
    public static TransactionDTO fromEntity(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setType(transaction.getType().name());
        dto.setTypeDisplay(transaction.getType().getDisplayName());
        dto.setStatus(transaction.getStatus().name());
        dto.setStatusDisplay(transaction.getStatus().getDisplayName());
        dto.setAmount(transaction.getAmount());
        dto.setCurrency(transaction.getCurrency().name());
        dto.setAmountDisplay(formatAmount(transaction.getAmount(), transaction.getCurrency()));
        dto.setPaymentMethod(transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : null);
        dto.setPaymentMethodDisplay(transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().getDisplayName() : null);
        dto.setDescription(transaction.getDescription());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setCompletedAt(transaction.getCompletedAt());
        dto.setTimeDisplay(getTimeDisplay(transaction.getCreatedAt()));
        return dto;
    }
    
    private static String formatAmount(BigDecimal amount, Currency currency) {
        return String.format("%.2f %s", amount, currency.getSymbol());
    }
    
    private static String getTimeDisplay(LocalDateTime dateTime) {
        // Заглушка для отображения времени
        return "недавно";
    }
} 