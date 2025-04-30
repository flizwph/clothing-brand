package com.brand.backend.domain.order.model;

import com.brand.backend.domain.product.model.DigitalProduct;
import com.brand.backend.domain.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Модель позиции цифрового заказа
 */
@Entity
@Table(name = "digital_order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "digital_product_id")
    private DigitalProduct digitalProduct;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private DigitalOrder order;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price")
    private Double price;
    
    @Column(name = "activation_code")
    private String activationCode;
    
    @Column(name = "activation_date")
    private LocalDateTime activationDate;
    
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;
    
    /**
     * Вычислить общую стоимость позиции
     * 
     * @return общая стоимость позиции
     */
    public Double getTotalPrice() {
        return price * quantity;
    }
    
    /**
     * Проверяет, активен ли цифровой продукт
     * @return true, если продукт активен и срок его действия не истек
     */
    public boolean isActive() {
        if (activationDate == null) {
            return false;
        }
        
        if (expirationDate == null) {
            return true; // Бессрочный доступ
        }
        
        return LocalDateTime.now().isBefore(expirationDate);
    }
    
    /**
     * Активирует цифровой продукт
     */
    public void activate() {
        this.activationDate = LocalDateTime.now();
        
        // Если у продукта есть период доступа, устанавливаем дату истечения
        if (digitalProduct != null && digitalProduct.getAccessPeriodDays() != null) {
            this.expirationDate = this.activationDate.plusDays(digitalProduct.getAccessPeriodDays());
        }
    }
    
    /**
     * Активирует цифровой продукт с заданным периодом доступа
     * @param accessPeriodDays количество дней доступа
     */
    public void activate(Integer accessPeriodDays) {
        this.activationDate = LocalDateTime.now();
        
        if (accessPeriodDays != null) {
            this.expirationDate = this.activationDate.plusDays(accessPeriodDays);
        }
    }
} 