package com.brand.backend.application.payment.service;

import com.brand.backend.domain.payment.model.Transaction;
import com.brand.backend.domain.payment.model.TransactionStatus;
import com.brand.backend.domain.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для администрирования транзакций (пополнений)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionAdminService {

    private final TransactionRepository transactionRepository;
    private final BalanceService balanceService;

    /**
     * Получает детали транзакции по ID
     */
    @Transactional(readOnly = true)
    public Optional<Transaction> getTransactionDetails(Long transactionId) {
        return transactionRepository.findByIdWithUser(transactionId);
    }

    /**
     * Подтверждает пополнение
     */
    @Transactional
    public Transaction confirmDeposit(String transactionCode, String adminInfo) {
        return balanceService.confirmDeposit(transactionCode, adminInfo);
    }

    /**
     * Отклоняет пополнение
     */
    @Transactional
    public Transaction rejectDeposit(String transactionCode, String reason, String adminInfo) {
        return balanceService.rejectDeposit(transactionCode, reason, adminInfo);
    }

    /**
     * Получает все транзакции
     */
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Получает транзакции по статусу
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    /**
     * Получает транзакции за сегодня
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsToday() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        return transactionRepository.findByCreatedAtAfter(startOfDay);
    }

    /**
     * Получает транзакции за неделю
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsThisWeek() {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        return transactionRepository.findByCreatedAtAfter(startOfWeek);
    }

    /**
     * Получает транзакции за месяц
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return transactionRepository.findByCreatedAtAfter(startOfMonth);
    }

    /**
     * Проверяет, можно ли подтвердить транзакцию
     */
    public boolean canConfirmTransaction(Transaction transaction) {
        return transaction != null && transaction.getStatus() == TransactionStatus.PENDING;
    }

    /**
     * Форматирует дату и время в читаемый вид
     */
    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return dateTime.format(formatter);
    }
    
    /**
     * Возвращает эмодзи для статуса транзакции
     */
    public String getStatusEmoji(TransactionStatus status) {
        return switch (status) {
            case PENDING -> "⏳";
            case COMPLETED -> "✅";
            case REJECTED -> "❌";
            case CANCELLED -> "🚫";
            default -> "";
        };
    }
} 