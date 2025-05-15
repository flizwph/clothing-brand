package com.brand.backend.presentation.rest.controller.payment;

import com.brand.backend.application.payment.service.BalanceService;
import com.brand.backend.domain.payment.model.Transaction;
import com.brand.backend.domain.payment.model.TransactionStatus;
import com.brand.backend.domain.payment.model.TransactionType;
import com.brand.backend.domain.payment.repository.TransactionRepository;
import com.brand.backend.domain.payment.exception.TransactionNotFoundException;
import com.brand.backend.domain.payment.exception.UnauthorizedTransactionAccessException;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.request.DepositRequest;
import com.brand.backend.presentation.dto.response.DepositInstructionsDto;
import com.brand.backend.presentation.dto.response.TransactionDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Контроллер для управления балансом
 */
@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
@Slf4j
public class BalanceController {
    
    private final BalanceService balanceService;
    private final TransactionRepository transactionRepository;
    
    /**
     * Получить текущий баланс пользователя
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserBalance(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Пользователь не авторизован");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        log.info("Запрос баланса для пользователя: {}", user.getUsername());
        
        BigDecimal balance = balanceService.getUserBalance(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("balance", balance);
        response.put("username", user.getUsername());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Создать запрос на пополнение баланса
     */
    @PostMapping("/deposit")
    public ResponseEntity<DepositInstructionsDto> createDepositRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DepositRequest request) {
        
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            DepositInstructionsDto errorResponse = DepositInstructionsDto.builder()
                    .message("Ошибка авторизации")
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        log.info("Запрос на пополнение баланса для пользователя {}: {} руб.", 
                user.getUsername(), request.getAmount());
        
        DepositInstructionsDto instructions = balanceService.createDepositRequest(user, request.getAmount());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(instructions);
    }
    
    /**
     * Получить историю транзакций пользователя
     */
    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionDto>> getUserTransactions(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Запрос истории транзакций для пользователя: {}", user.getUsername());
        
        Page<Transaction> transactions = transactionRepository.findByUserId(user.getId(), pageable);
        Page<TransactionDto> transactionDtos = transactions.map(TransactionDto::fromEntity);
        
        return ResponseEntity.ok(transactionDtos);
    }
    
    /**
     * Получить полную историю транзакций пользователя без пагинации
     */
    @GetMapping("/transactions/all")
    public ResponseEntity<List<TransactionDto>> getAllUserTransactions(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Запрос полной истории транзакций для пользователя: {}", user.getUsername());
        
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<TransactionDto> transactionDtos = transactions.stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(transactionDtos);
    }
    
    /**
     * Получить отфильтрованную историю транзакций пользователя
     */
    @GetMapping("/transactions/filtered")
    public ResponseEntity<List<TransactionDto>> getFilteredTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status) {
        
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Запрос отфильтрованных транзакций для пользователя {}: тип={}, статус={}", 
                user.getUsername(), type, status);
        
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        // Применяем фильтры
        List<Transaction> filteredTransactions = transactions.stream()
                .filter(transaction -> type == null || transaction.getType() == type)
                .filter(transaction -> status == null || transaction.getStatus() == status)
                .collect(Collectors.toList());
        
        List<TransactionDto> transactionDtos = filteredTransactions.stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(transactionDtos);
    }
    
    /**
     * Получить статистику по транзакциям пользователя
     */
    @GetMapping("/transactions/stats")
    public ResponseEntity<Map<String, Object>> getTransactionStats(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Запрос статистики по транзакциям для пользователя: {}", user.getUsername());
        
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        // Общее количество транзакций
        int totalTransactions = transactions.size();
        
        // Количество транзакций по статусам
        Map<TransactionStatus, Long> countByStatus = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getStatus, Collectors.counting()));
        
        // Количество транзакций по типам
        Map<TransactionType, Long> countByType = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getType, Collectors.counting()));
        
        // Сумма завершенных пополнений
        BigDecimal totalDeposits = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT && t.getStatus() == TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Сумма завершенных списаний
        BigDecimal totalWithdrawals = transactions.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL && t.getStatus() == TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Сумма оплаченных заказов
        BigDecimal totalOrderPayments = transactions.stream()
                .filter(t -> t.getType() == TransactionType.ORDER_PAYMENT && t.getStatus() == TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Количество ожидающих подтверждения транзакций
        long pendingTransactions = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PENDING)
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTransactions", totalTransactions);
        stats.put("countByStatus", countByStatus);
        stats.put("countByType", countByType);
        stats.put("totalDeposits", totalDeposits);
        stats.put("totalWithdrawals", totalWithdrawals);
        stats.put("totalOrderPayments", totalOrderPayments);
        stats.put("pendingTransactions", pendingTransactions);
        stats.put("balance", user.getBalance());
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Получить информацию о конкретной транзакции
     */
    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionDto> getTransactionDetails(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Запрос информации о транзакции {} для пользователя: {}", id, user.getUsername());
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        
        // Проверяем, что транзакция принадлежит текущему пользователю
        if (!transaction.getUser().getId().equals(user.getId())) {
            log.warn("Попытка доступа к чужой транзакции: {} пользователем {}", id, user.getUsername());
            throw new UnauthorizedTransactionAccessException(user.getUsername(), transaction.getTransactionCode());
        }
        
        return ResponseEntity.ok(TransactionDto.fromEntity(transaction));
    }
    
    /**
     * Отменить пополнение баланса
     */
    @PostMapping("/transactions/{id}/cancel")
    public ResponseEntity<TransactionDto> cancelTransaction(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Запрос на отмену транзакции {} для пользователя: {}", id, user.getUsername());
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        
        // Проверяем, что транзакция принадлежит текущему пользователю
        if (!transaction.getUser().getId().equals(user.getId())) {
            log.warn("Попытка отмены чужой транзакции: {} пользователем {}", id, user.getUsername());
            throw new UnauthorizedTransactionAccessException(user.getUsername(), transaction.getTransactionCode());
        }
        
        Transaction canceledTransaction = balanceService.cancelDeposit(transaction.getTransactionCode(), user);
        
        return ResponseEntity.ok(TransactionDto.fromEntity(canceledTransaction));
    }
    
    /**
     * Получить информацию о транзакции по её коду
     */
    @GetMapping("/transactions/code/{transactionCode}")
    public ResponseEntity<TransactionDto> getTransactionByCode(
            @AuthenticationPrincipal User user,
            @PathVariable String transactionCode) {
        
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Запрос информации о транзакции с кодом {} для пользователя: {}", 
                transactionCode, user.getUsername());
        
        Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new TransactionNotFoundException(transactionCode));
        
        // Проверяем, что транзакция принадлежит текущему пользователю
        if (!transaction.getUser().getId().equals(user.getId())) {
            log.warn("Попытка доступа к чужой транзакции: {} пользователем {}", 
                    transactionCode, user.getUsername());
            throw new UnauthorizedTransactionAccessException(user.getUsername(), transactionCode);
        }
        
        return ResponseEntity.ok(TransactionDto.fromEntity(transaction));
    }
} 