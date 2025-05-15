package com.brand.backend.application.payment.service;

import com.brand.backend.domain.payment.model.Transaction;
import com.brand.backend.domain.payment.model.TransactionStatus;
import com.brand.backend.domain.payment.model.TransactionType;
import com.brand.backend.domain.payment.repository.TransactionRepository;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.infrastructure.config.PaymentProperties;
import com.brand.backend.presentation.dto.response.DepositInstructionsDto;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import com.brand.backend.domain.payment.exception.TransactionNotFoundException;
import com.brand.backend.domain.payment.exception.InvalidTransactionStateException;
import com.brand.backend.domain.payment.exception.UnauthorizedTransactionAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Сервис для управления балансом пользователя
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {
    
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentProperties paymentProperties;
    private final AdminNotificationService adminNotificationService;
    private final TelegramBotService telegramBotService;
    
    /**
     * Создать запрос на пополнение баланса
     */
    @Transactional
    public DepositInstructionsDto createDepositRequest(User user, BigDecimal amount) {
        log.info("Создание запроса на пополнение баланса для пользователя {}: {} руб.", user.getUsername(), amount);
        
        String transactionCode = generateTransactionCode(user.getId());
        
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .transactionCode(transactionCode)
                .createdAt(LocalDateTime.now())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        // Уведомляем администраторов о новом запросе на пополнение
        adminNotificationService.notifyNewDepositRequest(transaction);
        
        return DepositInstructionsDto.builder()
                .transactionId(transaction.getId())
                .amount(amount)
                .transactionCode(transactionCode)
                .cardNumber(paymentProperties.getCardNumber())
                .cardholderName(paymentProperties.getCardholderName())
                .bankName(paymentProperties.getBankName())
                .message("Для пополнения баланса переведите указанную сумму на карту, обязательно указав код транзакции в комментарии к переводу.")
                .build();
    }
    
    /**
     * Подтвердить пополнение баланса
     */
    @Transactional
    public Transaction confirmDeposit(String transactionCode, String adminUsername) {
        log.info("Подтверждение пополнения баланса с кодом транзакции: {}", transactionCode);
        
        Transaction transaction = transactionRepository.findByTransactionCodeWithUser(transactionCode)
                .orElseThrow(() -> new TransactionNotFoundException(transactionCode));
        
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Попытка подтвердить транзакцию в статусе {}: {}", transaction.getStatus(), transactionCode);
            throw new InvalidTransactionStateException(transaction.getStatus(), TransactionStatus.PENDING);
        }
        
        User user = transaction.getUser();
        
        // Увеличиваем баланс пользователя
        user.setBalance(user.getBalance().add(transaction.getAmount()));
        userRepository.save(user);
        
        // Обновляем статус транзакции
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setAdminComment("Подтверждено администратором: " + adminUsername);
        
        Transaction updatedTransaction = transactionRepository.save(transaction);
        
        // Отправляем уведомление пользователю через Telegram
        telegramBotService.sendTransactionStatusNotification(user, updatedTransaction);
        
        return updatedTransaction;
    }
    
    /**
     * Отклонить пополнение баланса
     */
    @Transactional
    public Transaction rejectDeposit(String transactionCode, String reason, String adminUsername) {
        log.info("Отклонение пополнения баланса с кодом транзакции: {}", transactionCode);
        
        Transaction transaction = transactionRepository.findByTransactionCodeWithUser(transactionCode)
                .orElseThrow(() -> new TransactionNotFoundException(transactionCode));
        
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Попытка отклонить транзакцию в статусе {}: {}", transaction.getStatus(), transactionCode);
            throw new InvalidTransactionStateException(transaction.getStatus(), TransactionStatus.PENDING);
        }
        
        // Обновляем статус транзакции
        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setAdminComment(reason + " (" + adminUsername + ")");
        
        Transaction updatedTransaction = transactionRepository.save(transaction);
        
        // Отправляем уведомление пользователю через Telegram
        telegramBotService.sendTransactionStatusNotification(transaction.getUser(), updatedTransaction);
        
        return updatedTransaction;
    }
    
    /**
     * Отменить пополнение баланса
     */
    @Transactional
    public Transaction cancelDeposit(String transactionCode, User user) {
        log.info("Отмена пополнения баланса с кодом транзакции: {}", transactionCode);
        
        Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new TransactionNotFoundException(transactionCode));
        
        if (!transaction.getUser().getId().equals(user.getId())) {
            log.warn("Попытка отменить чужую транзакцию: {} пользователем {}", transactionCode, user.getUsername());
            throw new UnauthorizedTransactionAccessException(user.getUsername(), transactionCode);
        }
        
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Попытка отменить транзакцию в статусе {}: {}", transaction.getStatus(), transactionCode);
            throw new InvalidTransactionStateException(transaction.getStatus(), "отмена");
        }
        
        // Обновляем статус транзакции
        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setUpdatedAt(LocalDateTime.now());
        
        Transaction updatedTransaction = transactionRepository.save(transaction);
        
        // Отправляем уведомление пользователю через Telegram
        telegramBotService.sendTransactionStatusNotification(user, updatedTransaction);
        
        return updatedTransaction;
    }
    
    /**
     * Получить баланс пользователя
     */
    public BigDecimal getUserBalance(User user) {
        return user.getBalance();
    }
    
    /**
     * Проверить достаточность средств на балансе
     */
    public boolean hasEnoughBalance(User user, BigDecimal amount) {
        return user.getBalance().compareTo(amount) >= 0;
    }
    
    /**
     * Сгенерировать уникальный код транзакции
     */
    private String generateTransactionCode(Long userId) {
        String code;
        do {
            // Формат: TR-{последние 2 цифры userId}-{random_string}-{timestamp}
            code = String.format("TR-%02d-%s-%d", 
                    userId % 100,
                    RandomStringUtils.randomAlphanumeric(6).toUpperCase(),
                    System.currentTimeMillis() % 1000000);
        } while (transactionRepository.existsByTransactionCode(code));
        
        return code;
    }
} 