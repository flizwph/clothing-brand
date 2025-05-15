package com.brand.backend.presentation.rest.controller.admin;

import com.brand.backend.application.payment.service.BalanceService;
import com.brand.backend.domain.payment.model.Transaction;
import com.brand.backend.domain.payment.model.TransactionStatus;
import com.brand.backend.domain.payment.repository.TransactionRepository;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.response.TransactionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для администраторов по управлению транзакциями
 */
@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransactionController {
    
    private final TransactionRepository transactionRepository;
    private final BalanceService balanceService;
    
    /**
     * Получить все ожидающие транзакции
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<TransactionDto>> getPendingTransactions(Pageable pageable) {
        log.info("Запрос ожидающих транзакций");
        
        Page<Transaction> transactions = transactionRepository.findByStatus(TransactionStatus.PENDING, pageable);
        Page<TransactionDto> transactionDtos = transactions.map(TransactionDto::fromEntity);
        
        return ResponseEntity.ok(transactionDtos);
    }
    
    /**
     * Подтвердить транзакцию
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<TransactionDto> confirmTransaction(
            @PathVariable Long id, 
            @AuthenticationPrincipal User admin) {
        
        log.info("Подтверждение транзакции {} администратором {}", id, admin.getUsername());
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Транзакция не найдена"));
        
        Transaction confirmedTransaction = balanceService.confirmDeposit(
                transaction.getTransactionCode(), admin.getUsername());
        
        return ResponseEntity.ok(TransactionDto.fromEntity(confirmedTransaction));
    }
    
    /**
     * Отклонить транзакцию
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<TransactionDto> rejectTransaction(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal User admin) {
        
        log.info("Отклонение транзакции {} администратором {}", id, admin.getUsername());
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Транзакция не найдена"));
        
        Transaction rejectedTransaction = balanceService.rejectDeposit(
                transaction.getTransactionCode(), reason, admin.getUsername());
        
        return ResponseEntity.ok(TransactionDto.fromEntity(rejectedTransaction));
    }
    
    /**
     * Получить детали транзакции
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTransactionDetails(@PathVariable Long id) {
        log.info("Запрос деталей транзакции {}", id);
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Транзакция не найдена"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("transaction", TransactionDto.fromEntity(transaction));
        response.put("user", Map.of(
                "id", transaction.getUser().getId(),
                "username", transaction.getUser().getUsername(),
                "email", transaction.getUser().getEmail(),
                "balance", transaction.getUser().getBalance()
        ));
        
        return ResponseEntity.ok(response);
    }
} 