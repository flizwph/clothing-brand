package ru.escapismart.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @Column(name = "vk_id")
    private Long vkId;
    
    @Column(name = "last_state")
    private String lastState;
    
    @Column(name = "last_interaction")
    private LocalDateTime lastInteraction;
    
    @Column(name = "balance")
    private Double balance;
    
    @Column(name = "total_spent")
    private Double totalSpent;
    
    @Column(name = "discount_percent")
    private Integer discountPercent;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "shipping_address")
    private String shippingAddress;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "notification_enabled")
    private Boolean notificationEnabled;
    
    @Column(name = "last_input_prompt")
    private String lastInputPrompt;
    
    @Column(name = "step_data")
    private String stepData;
    
    @ElementCollection
    @CollectionTable(name = "user_used_promocodes", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "promocode")
    private Set<String> usedPromocodes = new HashSet<>();
    
    public User() {
        this.balance = 0.0;
        this.totalSpent = 0.0;
        this.discountPercent = 0;
        this.notificationEnabled = true;
    }
    
    public User(Long vkId, String lastState) {
        this();
        this.vkId = vkId;
        this.lastState = lastState;
        this.lastInteraction = LocalDateTime.now();
    }
    
    public Long getId() {
        return vkId;
    }
    
    public Long getVkId() {
        return vkId;
    }
    
    public void setVkId(Long vkId) {
        this.vkId = vkId;
    }
    
    public String getLastState() {
        return lastState;
    }
    
    public void setLastState(String lastState) {
        this.lastState = lastState;
    }
    
    public LocalDateTime getLastInteraction() {
        return lastInteraction;
    }
    
    public void setLastInteraction(LocalDateTime lastInteraction) {
        this.lastInteraction = lastInteraction;
    }
    
    public void setLastInteractionTime(Date date) {
        if (date != null) {
            this.lastInteraction = date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
        }
    }
    
    @PreUpdate
    @PrePersist
    public void updateLastInteraction() {
        this.lastInteraction = LocalDateTime.now();
    }
    
    public Double getBalance() {
        return balance;
    }
    
    public void setBalance(Double balance) {
        this.balance = balance;
    }
    
    public Double getTotalSpent() {
        return totalSpent;
    }
    
    public void setTotalSpent(Double totalSpent) {
        this.totalSpent = totalSpent;
    }
    
    public Integer getDiscountPercent() {
        return discountPercent;
    }
    
    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public Boolean getNotificationEnabled() {
        return notificationEnabled;
    }
    
    public void setNotificationEnabled(Boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }
    
    public Set<String> getUsedPromocodes() {
        return usedPromocodes;
    }
    
    public void setUsedPromocodes(Set<String> usedPromocodes) {
        this.usedPromocodes = usedPromocodes;
    }
    
    public void addUsedPromocode(String promocode) {
        if (this.usedPromocodes == null) {
            this.usedPromocodes = new HashSet<>();
        }
        this.usedPromocodes.add(promocode);
    }
    
    public boolean hasUsedPromocode(String promocode) {
        return this.usedPromocodes != null && this.usedPromocodes.contains(promocode);
    }
    
    public String getLastInputPrompt() {
        return lastInputPrompt;
    }
    
    public void setLastInputPrompt(String lastInputPrompt) {
        this.lastInputPrompt = lastInputPrompt;
    }
    
    public String getStepData() {
        return stepData;
    }
    
    public void setStepData(String stepData) {
        this.stepData = stepData;
    }
    
    public void addToBalance(Double amount) {
        if (this.balance == null) {
            this.balance = 0.0;
        }
        this.balance += amount;
    }
    
    public boolean deductFromBalance(Double amount) {
        if (this.balance == null) {
            this.balance = 0.0;
        }
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }
    
    public void addToTotalSpent(Double amount) {
        if (this.totalSpent == null) {
            this.totalSpent = 0.0;
        }
        this.totalSpent += amount;
        
        // Автоматически обновляем процент скидки на основе потраченной суммы
        updateDiscountPercent();
    }
    
    private void updateDiscountPercent() {
        if (this.totalSpent >= 50000) {
            this.discountPercent = 15;
        } else if (this.totalSpent >= 20000) {
            this.discountPercent = 10;
        } else if (this.totalSpent >= 5000) {
            this.discountPercent = 5;
        } else {
            this.discountPercent = 0;
        }
    }
    
    /**
     * Получить информацию о профиле пользователя в текстовом виде
     * @return строковое представление профиля пользователя
     */
    public String getProfileInfo() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("🔸 Профиль пользователя 🔸\n\n");
        
        // Имя и фамилия
        if (firstName != null && !firstName.isEmpty()) {
            sb.append("👤 Имя: ").append(firstName);
            
            if (lastName != null && !lastName.isEmpty()) {
                sb.append(" ").append(lastName);
            }
            
            sb.append("\n");
        }
        
        // Баланс
        if (balance != null) {
            sb.append("💰 Баланс: ").append(String.format("%.2f", balance)).append(" руб.\n");
        }
        
        // Скидка постоянного клиента
        if (discountPercent != null && discountPercent > 0) {
            sb.append("🎁 Ваша скидка: ").append(discountPercent).append("%\n");
        }
        
        // Потрачено всего
        if (totalSpent != null && totalSpent > 0) {
            sb.append("💳 Всего потрачено: ").append(String.format("%.2f", totalSpent)).append(" руб.\n");
        }
        
        // Контактная информация
        sb.append("\n📱 Контактная информация:\n");
        
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            sb.append("☎️ Телефон: ").append(phoneNumber).append("\n");
        } else {
            sb.append("☎️ Телефон: не указан\n");
        }
        
        if (email != null && !email.isEmpty()) {
            sb.append("📧 Email: ").append(email).append("\n");
        } else {
            sb.append("📧 Email: не указан\n");
        }
        
        if (shippingAddress != null && !shippingAddress.isEmpty()) {
            sb.append("🏠 Адрес доставки: ").append(shippingAddress).append("\n");
        } else {
            sb.append("🏠 Адрес доставки: не указан\n");
        }
        
        // Уведомления
        sb.append("\n🔔 Уведомления: ").append(notificationEnabled ? "включены" : "отключены").append("\n");
        
        return sb.toString();
    }
} 