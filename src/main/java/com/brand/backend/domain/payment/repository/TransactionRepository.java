package com.brand.backend.domain.payment.repository;

import com.brand.backend.domain.payment.model.Transaction;
import com.brand.backend.domain.payment.model.TransactionStatus;
import com.brand.backend.domain.payment.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с транзакциями
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Найти все транзакции пользователя
     */
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Найти транзакции пользователя с пагинацией
     */
    Page<Transaction> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Найти транзакции по статусу
     */
    List<Transaction> findByStatus(TransactionStatus status);
    
    /**
     * Найти транзакции по статусу с пагинацией
     */
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
    
    /**
     * Найти транзакции по типу и статусу
     */
    List<Transaction> findByTypeAndStatus(TransactionType type, TransactionStatus status);
    
    /**
     * Найти транзакцию по уникальному коду
     */
    Optional<Transaction> findByTransactionCode(String transactionCode);
    
    /**
     * Проверить существование транзакции по коду
     */
    boolean existsByTransactionCode(String transactionCode);
    
    /**
     * Найти транзакцию по ID заказа и типу
     */
    Optional<Transaction> findByOrderIdAndType(Long orderId, TransactionType type);
    
    /**
     * Найти транзакцию по ID с жадной загрузкой пользователя
     */
    @Query("SELECT t FROM Transaction t JOIN FETCH t.user WHERE t.id = :id")
    Optional<Transaction> findByIdWithUser(@Param("id") Long id);
    
    /**
     * Найти транзакцию по коду с жадной загрузкой пользователя
     */
    @Query("SELECT t FROM Transaction t JOIN FETCH t.user WHERE t.transactionCode = :code")
    Optional<Transaction> findByTransactionCodeWithUser(@Param("code") String transactionCode);
    
    List<Transaction> findByCreatedAtAfter(LocalDateTime date);
    
    List<Transaction> findAll();
} 