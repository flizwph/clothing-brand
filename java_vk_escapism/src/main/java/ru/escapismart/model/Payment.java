package ru.escapismart.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Модель платежа в системе VK Escapism Bot
 * Хранит всю информацию о платежах пользователей, включая статус, сумму и метаданные
 */
@Entity
@Table(name = "payments")
public class Payment {
    
    /**
     * Уникальный идентификатор платежа
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Идентификатор пользователя VK, совершающего платеж
     */
    @Column(name = "user_vk_id", nullable = false)
    private Long userVkId;
    
    /**
     * Идентификатор заказа, связанного с платежом
     */
    @Column(name = "order_id")
    private String orderId;
    
    /**
     * Сумма платежа
     */
    @Column(nullable = false)
    private BigDecimal amount;
    
    /**
     * Номер аккаунта/кошелька для оплаты
     */
    @Column(name = "payment_account")
    private String paymentAccount;
    
    /**
     * Уникальный комментарий к платежу для идентификации
     */
    @Column(name = "payment_comment", unique = true)
    private String paymentComment;
    
    /**
     * Статус платежа: PENDING, COMPLETED, CANCELLED и т.д.
     */
    @Column(nullable = false)
    private String status;
    
    /**
     * Дата и время создания платежа
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Дата и время последнего обновления платежа
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Метод, автоматически вызываемый перед сохранением нового объекта
     * Устанавливает текущее время как дату создания и обновления платежа
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Метод, автоматически вызываемый перед обновлением объекта
     * Обновляет дату последнего изменения платежа
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserVkId() {
        return userVkId;
    }
    
    public void setUserVkId(Long userVkId) {
        this.userVkId = userVkId;
    }
    
    /**
     * Устанавливает ID пользователя (метод-адаптер для совместимости)
     */
    public void setUserId(Long userId) {
        this.userVkId = userId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    /**
     * Устанавливает ID заказа (метод-адаптер для числового типа)
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId != null ? orderId.toString() : null;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    /**
     * Получает сумму платежа как double (для совместимости)
     */
    public double getAmountValue() {
        return amount != null ? amount.doubleValue() : 0.0;
    }
    
    public String getPaymentAccount() {
        return paymentAccount;
    }
    
    public void setPaymentAccount(String paymentAccount) {
        this.paymentAccount = paymentAccount;
    }
    
    public String getPaymentComment() {
        return paymentComment;
    }
    
    public void setPaymentComment(String paymentComment) {
        this.paymentComment = paymentComment;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Получает дату создания как Date (для совместимости)
     */
    public Date getCreatedAtAsDate() {
        return createdAt != null ? 
            Date.from(createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant()) : null;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Устанавливает дату создания из Date (для совместимости)
     */
    public void setCreatedAt(Date date) {
        if (date != null) {
            this.createdAt = date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
        }
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Устанавливает дату обновления из Date (для совместимости)
     */
    public void setUpdatedAt(Date date) {
        if (date != null) {
            this.updatedAt = date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
        }
    }
    
    /**
     * Метод для проверки наличия значения (замена isPresent())
     */
    public boolean isPresent() {
        return id != null;
    }
    
    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", userVkId=" + userVkId +
                ", orderId='" + orderId + '\'' +
                ", amount=" + amount +
                ", paymentComment='" + paymentComment + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
} 