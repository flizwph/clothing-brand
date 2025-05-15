package ru.escapismart.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Модель для хранения подписок пользователей на уведомления об изменении курсов криптовалют
 */
@Entity
@Table(name = "crypto_alerts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "token_symbol"}))
public class CryptoAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "token_symbol", nullable = false)
    private String tokenSymbol;
    
    @Column(name = "threshold")
    private double threshold;
    
    @Column(name = "last_price")
    private double lastPrice;
    
    @Column(name = "last_notification")
    private LocalDateTime lastNotification;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "is_active")
    private boolean active;
    
    public CryptoAlert() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }
    
    public CryptoAlert(Long userId, String tokenSymbol, double threshold) {
        this();
        this.userId = userId;
        this.tokenSymbol = tokenSymbol.toUpperCase();
        this.threshold = threshold;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getTokenSymbol() {
        return tokenSymbol;
    }
    
    public void setTokenSymbol(String tokenSymbol) {
        this.tokenSymbol = tokenSymbol.toUpperCase();
    }
    
    public double getThreshold() {
        return threshold;
    }
    
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    
    public double getLastPrice() {
        return lastPrice;
    }
    
    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }
    
    public LocalDateTime getLastNotification() {
        return lastNotification;
    }
    
    public void setLastNotification(LocalDateTime lastNotification) {
        this.lastNotification = lastNotification;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Проверка, нужно ли отправлять уведомление при текущей цене
     * @param currentPrice Текущая цена криптовалюты
     * @return true если изменение превышает порог
     */
    public boolean shouldNotify(double currentPrice) {
        if (lastPrice == 0) {
            return false; // Первое измерение, не уведомляем
        }
        
        double percentChange = Math.abs((currentPrice - lastPrice) / lastPrice * 100);
        return percentChange >= threshold;
    }
    
    /**
     * Обновить последнюю цену и время уведомления
     * @param currentPrice Текущая цена
     * @param shouldUpdateNotificationTime Обновлять ли время последнего уведомления
     */
    public void updatePrice(double currentPrice, boolean shouldUpdateNotificationTime) {
        this.lastPrice = currentPrice;
        if (shouldUpdateNotificationTime) {
            this.lastNotification = LocalDateTime.now();
        }
    }
} 